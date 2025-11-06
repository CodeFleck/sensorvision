# Publishing Java SDK to Maven Central

This guide explains how to publish the SensorVision Java SDK to Maven Central using the **new Central Portal** (as of January 2024).

**⚠️ Important**: The old OSSRH/Jira process has been decommissioned. Use the new Central Portal instead.

## Prerequisites

### 1. Maven Central Portal Account

**Register at Central Portal**: https://central.sonatype.com/

1. Click "Sign Up" and create an account
2. Verify your email address
3. Login to the Central Portal

### 2. Claim Your Namespace

**Method 1: GitHub Verification (Recommended)**

For `io.github.YOUR_USERNAME` namespace:
- No verification needed if publishing under `io.github.codefleck`
- Automatically verified via GitHub OAuth

For custom namespace `io.sensorvision`:
1. Navigate to: https://central.sonatype.com/publishing/namespaces
2. Click "Add Namespace"
3. Enter: `io.sensorvision`
4. Choose verification method:
   - **GitHub Repository**: Add `OSSRH-xxxxx` as a topic to https://github.com/CodeFleck/sensorvision
   - **DNS TXT Record**: Add TXT record to `sensorvision.io` domain
   - **Email**: Send verification email from `@sensorvision.io` address

**Method 2: DNS Verification**

Add TXT record to your domain:
```
Host: _maven-central-verification
Value: [verification-code-from-portal]
```

**Method 3: Support Ticket**

If you don't control the domain or GitHub org, email: central-support@sonatype.com

### 3. Generate User Token

**In Central Portal**:
1. Go to: https://central.sonatype.com/account
2. Click "Generate User Token"
3. Copy the generated username and password

**Save these credentials securely** - you'll need them for publishing.

### 4. GPG Setup

Install GPG:
```bash
# Windows (via Chocolatey)
choco install gpg4win

# macOS
brew install gnupg

# Linux (Ubuntu/Debian)
sudo apt-get install gnupg
```

Generate GPG key:
```bash
# Generate key pair
gpg --gen-key

# Use these settings:
# - Real name: SensorVision Team
# - Email: support@sensorvision.io
# - Passphrase: (choose a strong passphrase)

# List keys to get key ID
gpg --list-keys

# Publish key to keyserver (required by Maven Central)
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

### 5. Maven Settings

Create/edit `~/.m2/settings.xml`:

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
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <gpg.executable>gpg</gpg.executable>
        <gpg.passphrase>your-gpg-passphrase</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
</settings>
```

**Security Best Practice**: Use environment variables:
```bash
export CENTRAL_USERNAME="your-token-username"
export CENTRAL_PASSWORD="your-token-password"
export GPG_PASSPHRASE="your-gpg-passphrase"
```

Update settings.xml:
```xml
<server>
  <id>central</id>
  <username>${env.CENTRAL_USERNAME}</username>
  <password>${env.CENTRAL_PASSWORD}</password>
</server>
```

## Publishing Steps

### 1. Update Version

Edit `pom.xml`:
```xml
<version>0.1.0</version>  <!-- Update this -->
```

### 2. Run Tests

```bash
cd sensorvision-sdk-java
mvn clean test
```

Ensure all tests pass before publishing.

### 3. Build and Deploy

```bash
# Clean build
mvn clean package

# Deploy to Maven Central (requires GPG key)
mvn clean deploy

# Or with release profile
mvn clean deploy -P release

# With explicit GPG passphrase
mvn clean deploy -Dgpg.passphrase=your-passphrase
```

### 4. Verify Deployment

The new Central Portal provides immediate feedback:

1. **Check Deployment Status**:
   - Visit: https://central.sonatype.com/publishing/deployments
   - View your deployment status
   - Artifacts are validated automatically

2. **Published Artifacts**:
   - Central Portal: https://central.sonatype.com/artifact/io.sensorvision/sensorvision-sdk
   - Maven Central (synced within ~30 minutes): https://repo1.maven.org/maven2/io/sensorvision/sensorvision-sdk/

**Note**: The new process is much faster than the old OSSRH system (minutes vs hours).

### 5. Test Installation

Create a test project:
```xml
<dependency>
    <groupId>io.sensorvision</groupId>
    <artifactId>sensorvision-sdk</artifactId>
    <version>0.1.0</version>
</dependency>
```

```bash
mvn dependency:resolve
```

## Publishing Checklist

Before publishing a new version:

- [ ] Update version in `pom.xml`
- [ ] Update `CHANGELOG.md` with changes
- [ ] Run all tests: `mvn test`
- [ ] Build successfully: `mvn clean package`
- [ ] Verify Javadoc: `mvn javadoc:javadoc`
- [ ] Update README if API changed
- [ ] Create git tag: `git tag java-sdk-v0.1.0`
- [ ] Push tag: `git push origin java-sdk-v0.1.0`
- [ ] Deploy: `mvn clean deploy -P release`

## Troubleshooting

### GPG Signing Fails

**Issue**: `gpg: signing failed: Inappropriate ioctl for device`

**Solution**:
```bash
export GPG_TTY=$(tty)
```

Or use pinentry-mode in pom.xml (already configured).

### Authentication Failed

**Issue**: `401 Unauthorized`

**Solution**:
- Verify Sonatype credentials in `~/.m2/settings.xml`
- Check that your JIRA ticket for namespace claim is approved
- Ensure server `<id>ossrh</id>` matches in settings.xml and pom.xml

### GPG Key Not Found

**Issue**: `gpg: no default secret key: No secret key`

**Solution**:
```bash
# List keys
gpg --list-secret-keys

# If no keys, generate one
gpg --gen-key

# Set default key in settings.xml
<gpg.keyname>YOUR_KEY_ID</gpg.keyname>
```

### Repository Not Found

**Issue**: `Failed to deploy: repository element was not specified`

**Solution**: Ensure `distributionManagement` is properly configured in pom.xml (already done).

## Automated Publishing with GitHub Actions

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
        run: |
          echo "${{ secrets.GPG_PRIVATE_KEY }}" | gpg --batch --import

      - name: Create settings.xml
        run: |
          mkdir -p ~/.m2
          cat > ~/.m2/settings.xml <<EOF
          <settings>
            <servers>
              <server>
                <id>ossrh</id>
                <username>${{ secrets.OSSRH_USERNAME }}</username>
                <password>${{ secrets.OSSRH_PASSWORD }}</password>
              </server>
            </servers>
          </settings>
          EOF

      - name: Build and Deploy
        env:
          GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
        run: |
          cd sensorvision-sdk-java
          mvn clean deploy -P release -Dgpg.passphrase=$GPG_PASSPHRASE

      - name: Create GitHub Release
        uses: actions/create-release@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          tag_name: ${{ github.ref }}
          release_name: Java SDK ${{ github.ref }}
          draft: false
          prerelease: false
```

**Required GitHub Secrets**:
- `OSSRH_USERNAME` - Your Sonatype JIRA username
- `OSSRH_PASSWORD` - Your Sonatype JIRA password
- `GPG_PRIVATE_KEY` - Your GPG private key (export with `gpg --export-secret-keys -a YOUR_KEY_ID`)
- `GPG_PASSPHRASE` - Your GPG key passphrase

## Version Management

Follow [Semantic Versioning](https://semver.org/):
- **MAJOR**: Breaking changes (e.g., `1.0.0` → `2.0.0`)
- **MINOR**: New features, backwards-compatible (e.g., `1.0.0` → `1.1.0`)
- **PATCH**: Bug fixes, backwards-compatible (e.g., `1.0.0` → `1.0.1`)

## Resources

- **Maven Central**: https://central.sonatype.com/
- **Sonatype OSSRH Guide**: https://central.sonatype.org/publish/publish-guide/
- **GPG Guide**: https://central.sonatype.org/publish/requirements/gpg/
- **Maven Deploy Plugin**: https://maven.apache.org/plugins/maven-deploy-plugin/

---

**Status**: Ready for Maven Central deployment
**Last Updated**: 2025-01-15
