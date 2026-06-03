const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const vendorFonts = path.resolve(root, '../src/main/resources/static/vendor/fonts');
const interSrc = path.join(root, 'node_modules/@fontsource/inter/files');
const symbolsSrc = path.join(root, 'node_modules/@fontsource/material-symbols-outlined/files');

const interWeights = ['400', '600', '700'];
const symbolsFile = 'material-symbols-outlined-latin-400-normal.woff2';

function ensureDir(dir) {
    fs.mkdirSync(dir, { recursive: true });
}

function copyIfExists(from, to) {
    if (fs.existsSync(from)) {
        fs.copyFileSync(from, to);
        return true;
    }
    return false;
}

function removeExtraFonts(dir, keepNames) {
    if (!fs.existsSync(dir)) {
        return;
    }
    fs.readdirSync(dir).forEach(function (name) {
        if (!keepNames.has(name)) {
            fs.unlinkSync(path.join(dir, name));
        }
    });
}

function main() {
    const interDir = path.join(vendorFonts, 'inter');
    const symbolsDir = path.join(vendorFonts, 'material-symbols-outlined');
    ensureDir(interDir);
    ensureDir(symbolsDir);

    let copied = 0;
    const keepInter = new Set();
    const keepSymbols = new Set();

    interWeights.forEach(function (weight) {
        const file = 'inter-latin-' + weight + '-normal.woff2';
        keepInter.add(file);
        if (copyIfExists(path.join(interSrc, file), path.join(interDir, file))) {
            copied += 1;
        }
    });

    keepSymbols.add(symbolsFile);
    if (copyIfExists(path.join(symbolsSrc, symbolsFile), path.join(symbolsDir, symbolsFile))) {
        copied += 1;
    }

    removeExtraFonts(interDir, keepInter);
    removeExtraFonts(symbolsDir, keepSymbols);

    if (copied === 0) {
        console.error('No font files copied. Run npm install in frontend/ first.');
        process.exit(1);
    }

    console.log('Copied ' + copied + ' font file(s) (minimal set) to static/vendor/fonts/');
}

main();
