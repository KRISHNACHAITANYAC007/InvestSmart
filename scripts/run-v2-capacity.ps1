param([switch]$ProbeOnly)
$ErrorActionPreference = 'Stop'
if ($env:OS -ne 'Windows_NT') { throw 'This wrapper is Windows-only; use the documented isolated Linux runner.' }
Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
namespace InvestSmart.Release {
 public sealed class CapacityPowerRequest : IDisposable {
  [StructLayout(LayoutKind.Explicit, Size=32)] struct Reason {
   [FieldOffset(0)] public uint Version;
   [FieldOffset(4)] public uint Flags;
   [FieldOffset(8)] public IntPtr Text;
  }
  [StructLayout(LayoutKind.Sequential)] struct Status {
   public byte AC, Battery, Percent, Reserved;
   public uint Life, FullLife;
  }
  [DllImport("kernel32.dll",SetLastError=true)] static extern bool GetSystemPowerStatus(out Status status);
  [DllImport("kernel32.dll",SetLastError=true)] static extern IntPtr PowerCreateRequest(ref Reason reason);
  [DllImport("kernel32.dll",SetLastError=true)] static extern bool PowerSetRequest(IntPtr handle,int type);
  [DllImport("kernel32.dll",SetLastError=true)] static extern bool PowerClearRequest(IntPtr handle,int type);
  [DllImport("kernel32.dll",SetLastError=true)] static extern bool CloseHandle(IntPtr handle);
  IntPtr handle; bool system,execution;
  public static bool OnAC() { Status s;return GetSystemPowerStatus(out s)&&s.AC==1; }
  public CapacityPowerRequest() {
   if(!OnAC())throw new InvalidOperationException("AC power is required for this Modern Standby benchmark.");
   var text=Marshal.StringToHGlobalUni("InvestSmart isolated capacity verification");
   try { var reason=new Reason { Version=0,Flags=1,Text=text };handle=PowerCreateRequest(ref reason); }
   finally { Marshal.FreeHGlobal(text); }
   if(handle==IntPtr.Zero||handle==new IntPtr(-1)) { handle=IntPtr.Zero;throw new InvalidOperationException("Cannot create scoped power request."); }
   system=PowerSetRequest(handle,1);execution=PowerSetRequest(handle,3);
   if(!system||!execution) { Dispose();throw new InvalidOperationException("System/execution power requests unavailable."); }
  }
  public void Dispose() { if(handle!=IntPtr.Zero) { if(execution)PowerClearRequest(handle,3);if(system)PowerClearRequest(handle,1);CloseHandle(handle);handle=IntPtr.Zero; } }
 }
}
'@
$releaseRoot = Split-Path -Parent $PSScriptRoot
$evidence = Join-Path $releaseRoot 'target/capacity-power-preflight.json'
[void][System.IO.Directory]::CreateDirectory((Split-Path -Parent $evidence))
$request = $null
try {
    $request = New-Object InvestSmart.Release.CapacityPowerRequest
    # Probe the exact scoped requests without changing any persistent power policy.
    1..10 | ForEach-Object { Start-Sleep -Seconds 1; if (![InvestSmart.Release.CapacityPowerRequest]::OnAC()) { throw 'AC power disconnected.' } }
    @{ at=[DateTime]::UtcNow.ToString('o'); onAC=$true; scopedSystemAndExecutionRequest=$true; probeSeconds=10; persistentPolicyChanged=$false; mode=$(if($ProbeOnly){'probe'}else{'full'}); fullLoadCompleted=$false } | ConvertTo-Json | Set-Content -LiteralPath $evidence
    if (!$ProbeOnly) {
        Push-Location $releaseRoot
        $oldSteady=$env:V2_LOAD_STEADY_SECONDS; $oldBurst=$env:V2_LOAD_BURST_SECONDS
        try {
            $env:V2_LOAD_STEADY_SECONDS='1800'; $env:V2_LOAD_BURST_SECONDS='300'
            & node scripts/check-v2-containers.mjs --capacity
            if ($LASTEXITCODE -ne 0) { throw 'Isolated capacity verification failed; inspect private logs.' }
            @{ at=[DateTime]::UtcNow.ToString('o'); onAC=[InvestSmart.Release.CapacityPowerRequest]::OnAC(); scopedSystemAndExecutionRequest=$true; persistentPolicyChanged=$false; fullLoadCompleted=$true; scope='Runner completion only; continuous metrics and independent capacity analysis remain required.' } | ConvertTo-Json | Set-Content -LiteralPath $evidence
        } finally { $env:V2_LOAD_STEADY_SECONDS=$oldSteady; $env:V2_LOAD_BURST_SECONDS=$oldBurst; Pop-Location }
    }
} catch {
    @{ at=[DateTime]::UtcNow.ToString('o'); onAC=[InvestSmart.Release.CapacityPowerRequest]::OnAC(); status='BLOCKED'; scopedRequestAccepted=($null -ne $request); persistentPolicyChanged=$false; reason='AC power/scoped request or isolated benchmark acceptance unavailable; no policy or security changes made.' } | ConvertTo-Json | Set-Content -LiteralPath $evidence
    Write-Error 'Capacity preflight/run blocked; see aggregate target/capacity-power-preflight.json and private test logs.'
    exit 1
} finally { if ($null -ne $request) { $request.Dispose() } }
