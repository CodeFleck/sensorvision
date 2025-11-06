# SDK Publishing Guide

This guide explains how to publish SensorVision SDKs to PyPI (Python) and npm (JavaScript/TypeScript).

---

## Current Status

✅ **Python SDK Published to PyPI** - https://pypi.org/project/sensorvision-sdk/
✅ **JavaScript SDK Published to npm** - https://www.npmjs.com/package/sensorvision-sdk

Both SDKs are now available on their respective package registries!

---

## Python SDK - Publishing to PyPI

### Prerequisites

**Windows (Recommended - use `py` launcher):**
```bash
py -m pip install build twine
```

**Linux/macOS:**
```bash
pip install build twine
# or
python3 -m pip install build twine
```

### Publishing Steps

1. **Update Version** (in `sensorvision-sdk/setup.py`):
   ```python
   version="0.1.0",  # Update this
   ```

2. **Build Distribution**:

   **Windows:**
   ```bash
   cd sensorvision-sdk
   py -m build
   ```

   **Linux/macOS:**
   ```bash
   cd sensorvision-sdk
   python3 -m build
   ```

   This creates:
   - `dist/sensorvision_sdk-0.1.0.tar.gz` (source distribution)
   - `dist/sensorvision_sdk-0.1.0-py3-none-any.whl` (wheel)

3. **Test Upload to TestPyPI** (Recommended First):

   **Windows:**
   ```bash
   py -m twine upload --repository testpypi dist/*
   ```

   **Linux/macOS:**
   ```bash
   python3 -m twine upload --repository testpypi dist/*
   ```

   Test installation:
   ```bash
   pip install --index-url https://test.pypi.org/simple/ sensorvision-sdk
   ```

4. **Upload to PyPI** (Production):

   **Windows:**
   ```bash
   py -m twine upload dist/*
   ```

   **Linux/macOS:**
   ```bash
   python3 -m twine upload dist/*
   ```

   Enter your PyPI credentials when prompted.

5. **Verify Installation**:
   ```bash
   pip install sensorvision-sdk
   ```

### PyPI Account Setup

1. Create account at https://pypi.org/account/register/
2. Enable 2FA (required for trusted publishers)
3. Generate API token at https://pypi.org/manage/account/token/
4. Configure credentials:
   ```bash
   # Option 1: Store in ~/.pypirc
   [pypi]
   username = __token__
   password = pypi-your-token-here

   # Option 2: Use environment variable
   export TWINE_USERNAME=__token__
   export TWINE_PASSWORD=pypi-your-token-here
   ```

### Automated Publishing with GitHub Actions

Create `.github/workflows/publish-python-sdk.yml`:

```yaml
name: Publish Python SDK to PyPI

on:
  push:
    tags:
      - 'python-sdk-v*'

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-python@v4
        with:
          python-version: '3.11'

      - name: Install build tools
        run: pip install build twine

      - name: Build package
        run: |
          cd sensorvision-sdk
          python -m build

      - name: Publish to PyPI
        env:
          TWINE_USERNAME: __token__
          TWINE_PASSWORD: ${{ secrets.PYPI_API_TOKEN }}
        run: |
          cd sensorvision-sdk
          python -m twine upload dist/*
```

Store PyPI token as GitHub secret: `PYPI_API_TOKEN`

**Publish with tag**:
```bash
git tag python-sdk-v0.1.0
git push origin python-sdk-v0.1.0
```

---

## JavaScript/TypeScript SDK - Publishing to npm

### Prerequisites

```bash
npm login  # Login to npm
```

### Publishing Steps

1. **Update Version** (in `sensorvision-sdk-js/package.json`):
   ```json
   {
     "version": "0.1.0"  // Update this
   }
   ```

2. **Build Package**:
   ```bash
   cd sensorvision-sdk-js
   npm install
   npm run build
   npm test  # Ensure tests pass
   ```

3. **Test Locally**:
   ```bash
   npm pack
   # This creates sensorvision-sdk-0.1.0.tgz

   # Test in another project
   cd /tmp/test-project
   npm install /path/to/sensorvision-sdk-0.1.0.tgz
   ```

4. **Publish to npm**:
   ```bash
   cd sensorvision-sdk-js
   npm publish
   ```

5. **Verify Installation**:
   ```bash
   npm install sensorvision-sdk
   ```

### npm Account Setup

1. Create account at https://www.npmjs.com/signup
2. Enable 2FA (recommended)
3. Generate access token: https://www.npmjs.com/settings/~/tokens
4. Login via CLI:
   ```bash
   npm login
   # Or use token
   npm set //registry.npmjs.org/:_authToken YOUR_TOKEN
   ```

### Automated Publishing with GitHub Actions

Create `.github/workflows/publish-js-sdk.yml`:

```yaml
name: Publish JS/TS SDK to npm

on:
  push:
    tags:
      - 'js-sdk-v*'

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
          registry-url: 'https://registry.npmjs.org'

      - name: Install dependencies
        run: |
          cd sensorvision-sdk-js
          npm install

      - name: Build package
        run: |
          cd sensorvision-sdk-js
          npm run build

      - name: Run tests
        run: |
          cd sensorvision-sdk-js
          npm test

      - name: Publish to npm
        env:
          NODE_AUTH_TOKEN: ${{ secrets.NPM_TOKEN }}
        run: |
          cd sensorvision-sdk-js
          npm publish
```

Store npm token as GitHub secret: `NPM_TOKEN`

**Publish with tag**:
```bash
git tag js-sdk-v0.1.0
git push origin js-sdk-v0.1.0
```

---

## Version Management Strategy

### Semantic Versioning

Follow [SemVer](https://semver.org/): `MAJOR.MINOR.PATCH`

- **MAJOR**: Breaking changes (e.g., `1.0.0` → `2.0.0`)
- **MINOR**: New features, backwards-compatible (e.g., `1.0.0` → `1.1.0`)
- **PATCH**: Bug fixes, backwards-compatible (e.g., `1.0.0` → `1.0.1`)

### Pre-Release Versions

For beta/alpha releases:
- `0.1.0-alpha.1`
- `0.1.0-beta.1`
- `0.1.0-rc.1` (release candidate)

```bash
# Python
python -m twine upload dist/*

# npm
npm publish --tag beta
```

### Release Checklist

Before publishing a new version:

- [ ] Update `CHANGELOG.md` with changes
- [ ] Update version in `setup.py` (Python) or `package.json` (JS)
- [ ] Run all tests: `pytest` (Python) or `npm test` (JS)
- [ ] Build successfully: `python -m build` (Python) or `npm run build` (JS)
- [ ] Update SDK README if API changed
- [ ] Create git tag: `git tag python-sdk-v0.1.0`
- [ ] Push tag: `git push origin python-sdk-v0.1.0`

---

## Package Naming & Organization

### PyPI Package Name
- **Package**: `sensorvision-sdk`
- **Import**: `from sensorvision import SensorVisionClient`
- **URL**: https://pypi.org/project/sensorvision-sdk/

### npm Package Name
- **Package**: `sensorvision-sdk`
- **Import**: `import { SensorVisionClient } from 'sensorvision-sdk'`
- **URL**: https://www.npmjs.com/package/sensorvision-sdk

### Scoped Packages (Alternative)

If unscoped names are taken, use organization scope:

**PyPI** (no scopes, use prefix):
- `sensorvision-sdk` → `sv-sdk` or `sensorvision-client`

**npm** (with @scope):
- `sensorvision-sdk` → `@sensorvision/sdk` or `@sensorvision/client`

---

## Current Installation Instructions

### Python SDK

**Install from GitHub**:
```bash
pip install git+https://github.com/CodeFleck/sensorvision.git#subdirectory=sensorvision-sdk
```

**After PyPI publish**:
```bash
pip install sensorvision-sdk
```

### JavaScript/TypeScript SDK

**Install from GitHub**:
```bash
npm install CodeFleck/sensorvision#main:sensorvision-sdk-js
```

**After npm publish**:
```bash
npm install sensorvision-sdk
```

---

## Maintenance & Updates

### Publishing Patch Updates

1. Fix bug in SDK
2. Increment patch version: `0.1.0` → `0.1.1`
3. Rebuild and publish
4. Users update: `pip install --upgrade sensorvision-sdk`

### Deprecation Policy

When introducing breaking changes:

1. **One version warning**: Deprecation warnings in v1.x
2. **Breaking change**: Remove in v2.0.0
3. **Migration guide**: Document in CHANGELOG

Example:
```python
# v1.5.0 - Add deprecation warning
def old_method():
    warnings.warn("old_method is deprecated, use new_method", DeprecationWarning)
    return new_method()

# v2.0.0 - Remove old_method
# (Users had time to migrate)
```

---

## Post-Publishing Tasks

After first publish to PyPI/npm:

1. **Update Main README**:
   ```markdown
   ### Official SDKs

   - [Python SDK](https://pypi.org/project/sensorvision-sdk/) - `pip install sensorvision-sdk`
   - [JavaScript/TypeScript SDK](https://www.npmjs.com/package/sensorvision-sdk) - `npm install sensorvision-sdk`
   ```

2. **Update Integration Wizard** (frontend):
   - Change installation instructions from GitHub to PyPI/npm
   - Update code generation templates

3. **Update Documentation**:
   - Quick Start guide
   - CLAUDE.md
   - SDK READMEs (remove "Coming Soon")

4. **Announce**:
   - GitHub release notes
   - Twitter/LinkedIn post
   - Discord/community channels
   - Blog post

---

## Troubleshooting

### "Package name already taken"

**PyPI**:
- Try: `sensorvision-client`, `sv-sdk`, `sensorvision-iot`
- Check availability: https://pypi.org/project/YOUR-NAME/

**npm**:
- Try scoped: `@sensorvision/sdk`, `@sensorvision/client`
- Try unscoped: `sv-sdk`, `sensorvision-client`, `sensorvision-iot`
- Check availability: https://www.npmjs.com/package/YOUR-NAME

### "Authentication failed"

- Verify API token is correct
- Check 2FA is enabled
- Ensure token has publish permissions

### "Build failed"

**Python (Windows):**
```bash
# Clear old builds
rmdir /s /q dist build
del /s /q *.egg-info
py -m build
```

**Python (Linux/macOS):**
```bash
# Clear old builds
rm -rf dist/ build/ *.egg-info
python3 -m build
```

**npm**:
```bash
# Clear cache and rebuild
rm -rf dist/ node_modules/
npm install
npm run build
```

---

## Resources

### Python/PyPI
- PyPI: https://pypi.org/
- TestPyPI: https://test.pypi.org/
- Packaging guide: https://packaging.python.org/
- Twine docs: https://twine.readthedocs.io/

### JavaScript/npm
- npm: https://www.npmjs.com/
- Publishing guide: https://docs.npmjs.com/packages-and-modules/contributing-packages-to-the-registry
- Semantic versioning: https://semver.org/

---

**Status**: Not yet published (available via GitHub)
**Priority**: Medium (publish after Sprint 2-3 when plugin ecosystem is ready)
**Blocker**: None - can publish anytime
