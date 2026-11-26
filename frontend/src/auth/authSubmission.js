// Read the submitted controls, including values supplied by browser autofill.
// Passwords must be preserved exactly; never trim or persist them here.
export function authSubmission(mode, form, token, challenge) {
    const value = name => String(form.get(name) ?? "");
    if (challenge) return { challenge, code: value("code").trim() };
    if (mode === "verify-email") return { token: token || "" };
    if (mode === "reset-password") return { token: token || "", password: value("password") };
    if (mode === "register") return { email: value("email"), name: value("name"), password: value("password") };
    if (mode === "login") return { email: value("email"), password: value("password") };
    return { email: value("email") };
}
