import test from 'node:test';
import assert from 'node:assert/strict';
import { heatmapLayout } from './heatmapLayout.js';
test('fifty equal-area tiles fill desktop and phone bounds without overlap', () => {
    for (const [width, height] of [[320, 180], [360, 420], [390, 420], [430, 420], [600, 280], [768, 520], [1024, 650], [1280, 650], [1440, 720], [1920, 900]]) {
        const tiles = heatmapLayout(50, width, height);
        assert.equal(tiles.length, 50);
        for (let i = 0; i < tiles.length; i++) {
            const a = tiles[i];
            assert.ok(a.left >= 0 && a.top >= 0 && a.left + a.width <= width + 1e-8 && a.top + a.height <= height + 1e-8);
            assert.ok(Math.abs(a.width * a.height - width * height / 50) < 1e-6);
            for (const b of tiles.slice(i + 1)) assert.ok(Math.min(a.left + a.width, b.left + b.width) - Math.max(a.left, b.left) < 1e-8 || Math.min(a.top + a.height, b.top + b.height) - Math.max(a.top, b.top) < 1e-8);
        }
    }
});
