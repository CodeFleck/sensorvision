# SDK Publishing Guide

This guide explains how to publish Industrial Cloud SDKs to PyPI (Python) and npm (JavaScript/TypeScript).

---

## Current Status

✅ **Python SDK v0.1.1 Published to PyPI** - https://pypi.org/project/indcloud-sdk/
✅ **JavaScript SDK v0.1.1 Published to npm** - https://www.npmjs.com/package/indcloud-sdk
🔄 **Java SDK v0.1.0 Ready for Maven Central** - Configuration complete, awaiting deployment

All three official SDKs are ready for production use!

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

1. **Update Version** (in `indcloud-sdk/setup.py`):
   ```python
   version="0.1.0",  # Update this
   ```

2. **Build Distribution**:

   **Windows:**
   ```bash
   cd indcloud-sdk
   py -m build
   ```

   **Linux/macOS:**
   ```bash
   cd indcloud-sdk
   python3 -m build
   ```

   This creates:
   - `dist/indcloud_sdk-0.1.0.tar.gz` (source distribution)
   - `dist/indcloud_sdk-0.1.0-py3-none-any.whl` (wheel)

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
   pip install --index-url https://test.pypi.org/simple/ indcloud-sdk
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
   pip install indcloud-sdk
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
          cd indcloud-sdk
          python -m build

      - name: Publish to PyPI
        env:
          TWINE_USERNAME: __token__
          TWINE_PASSWORD: ${{ secrets.PYPI_API_TOKEN }}
        run: |
          cd indcloud-sdk
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

1. **Update Version** (in `indcloud-sdk-js/package.json`):
   ```json
   {
     "version": "0.1.0"  // Update this
   }
   ```

2. **Build Package**:
   ```bash
   cd indcloud-sdk-js
   npm install
   npm run build
   npm test  # Ensure tests pass
   ```

3. **Test Locally**:
   ```bash
   npm pack
   # This creates indcloud-sdk-0.1.0.tgz

   # Test in another project
   cd /tmp/test-project
   npm install /path/to/indcloud-sdk-0.1.0.tgz
   ```

4. **Publish to npm**:
   ```bash
   cd indcloud-sdk-js
   npm publish
   ```

5. **Verify Installation**:
   ```bash
   npm install indcloud-sdk
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
          cd indcloud-sdk-js
          npm install

      - name: Build package
        run: |
          cd indcloud-sdk-js
          npm run build

      - name: Run tests
        run: |
          cd indcloud-sdk-js
          npm test

      - name: Publish to npm
        env:
          NODE_AUTH_TOKEN: ${{ secrets.NPM_TOKEN }}
        run: |
          cd indcloud-sdk-js
          npm publish
```

Store npm token as GitHub secret: `NPM_TOKEN`

**Publish with tag**:
```bash
git tag js-sdk-v0.1.0
git push origin js-sdk-v0.1.0
```

---

## Java SDK - Publishing to Maven Central

**⚠️ Important**: As of January 2024, use the new **Central Portal** (not the old OSSRH/Jira system).

### Prerequisites

**Create Maven Central Portal Account**:
1. Register at: https://central.sonatype.com/
2. Verify your email
3. Generate User Token at https://central.sonatype.com/account
4. Claim namespace via GitHub verification or DNS

**Setup GPG**:
```bash
# Install GPG (Windows via Chocolatey)
choco install gpg4win

# Generate key
gpg --gen-key

# Publish to keyserver
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
```

**Configure Maven Settings** (`~/.m2/settings.xml`):
```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>YOUR_CENTRAL_TOKEN_USERNAME</username>
      <password>YOUR_CENTRAL_TOKEN_PASSWORD</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>central</id>
      <properties>
        <gpg.executable>gpg</gpg.executable>
        <gpg.passphrase>your-gpg-passphrase</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
</settings>
```

### Publishing Steps

1. **Update Version** (in `pom.xml`):
   ```xml
   <version>0.1.0</version>  <!-- Update this -->
   ```

2. **Run Tests**:
   ```bash
   cd indcloud-sdk-java
   mvn clean test
   ```

3. **Deploy to Maven Central**:
   ```bash
   mvn clean deploy
   ```

4. **Verify Deployment**:
   - Central Portal: https://central.sonatype.com/publishing/deployments
   - Maven Central (synced within ~30 minutes): https://central.sonatype.com/artifact/io.indcloud/indcloud-sdk

### Automated Publishing with GitHub Actions

Create `.github/workflows/publish-java-sdk.yml`:

```yaml
name: Publish Java SDK to Maven Central

on:
  push:
    tags:
      - 'java-sdk-v*'

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      - name: Import GPG key
        run: echo "${{ secrets.GPG_PRIVATE_KEY }}" | gpg --batch --import

      - name: Deploy to Maven Central
        env:
          OSSRH_USERNAME: ${{ secrets.OSSRH_USERNAME }}
          OSSRH_PASSWORD: ${{ secrets.OSSRH_PASSWORD }}
          GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
        run: |
          cd indcloud-sdk-java
          mvn clean deploy -P release -Dgpg.passphrase=$GPG_PASSPHRASE
```

Store secrets: `OSSRH_USERNAME`, `OSSRH_PASSWORD`, `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`

**Publish with tag**:
```bash
git tag java-sdk-v0.1.0
git push origin java-sdk-v0.1.0
```

📚 **Full Publishing Guide**: [indcloud-sdk-java/PUBLISHING.md](../indcloud-sdk-java/PUBLISHING.md)

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
- **Package**: `indcloud-sdk`
- **Import**: `from indcloud import Industrial CloudClient`
- **URL**: https://pypi.org/project/indcloud-sdk/

### npm Package Name
- **Package**: `indcloud-sdk`
- **Import**: `import { Industrial CloudClient } from 'indcloud-sdk'`
- **URL**: https://www.npmjs.com/package/indcloud-sdk

### Scoped Packages (Alternative)

If unscoped names are taken, use organization scope:

**PyPI** (no scopes, use prefix):
- `indcloud-sdk` → `sv-sdk` or `indcloud-client`

**npm** (with @scope):
- `indcloud-sdk` → `@indcloud/sdk` or `@indcloud/client`

---

## Current Installation Instructions

### Python SDK

**Install from GitHub**:
```bash
pip install git+https://github.com/CodeFleck/indcloud.git#subdirectory=indcloud-sdk
```

**After PyPI publish**:
```bash
pip install indcloud-sdk
```

### JavaScript/TypeScript SDK

**Install from GitHub**:
```bash
npm install CodeFleck/indcloud#main:indcloud-sdk-js
```

**After npm publish**:
```bash
npm install indcloud-sdk
```

---

## Maintenance & Updates

### Publishing Patch Updates

1. Fix bug in SDK
2. Increment patch version: `0.1.0` → `0.1.1`
3. Rebuild and publish
4. Users update: `pip install --upgrade indcloud-sdk`

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

   - [Python SDK](https://pypi.org/project/indcloud-sdk/) - `pip install indcloud-sdk`
   - [JavaScript/TypeScript SDK](https://www.npmjs.com/package/indcloud-sdk) - `npm install indcloud-sdk`
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
- Try: `indcloud-client`, `sv-sdk`, `indcloud-iot`
- Check availability: https://pypi.org/project/YOUR-NAME/

**npm**:
- Try scoped: `@indcloud/sdk`, `@indcloud/client`
- Try unscoped: `sv-sdk`, `indcloud-client`, `indcloud-iot`
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
