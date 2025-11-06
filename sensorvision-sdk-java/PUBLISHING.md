# Publishing Java SDK to Maven Central

This guide explains how to publish the SensorVision Java SDK to Maven Central.

## Prerequisites

### 1. Sonatype OSSRH Account

Register at: https://issues.sonatype.org/secure/Signup!default.jspa

Create a JIRA ticket to claim your `io.sensorvision` namespace:
- **Project**: Community Support - Open Source Project Repository Hosting (OSSRH)
- **Issue Type**: New Project
- **Summary**: Request for io.sensorvision namespace
- **Group Id**: io.sensorvision
- **Project URL**: https://github.com/CodeFleck/sensorvision
- **SCM URL**: https://github.com/CodeFleck/sensorvision.git

### 2. GPG Setup

Install GPG:
```bash
# Windows (via Chocolatey)
choco install gpg4win

# Or download from: https://gpg4win.org/download.html
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

# Publish key to keyserver
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
```

### 3. Maven Settings

Create/edit `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>your-jira-username</username>
      <password>your-jira-password</password>
    </server>
  </servers>

  <profiles>
    <profile>
      <id>ossrh</id>
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

**Security Best Practice**: Use environment variables instead:
```bash
export OSSRH_USERNAME="your-jira-username"
export OSSRH_PASSWORD="your-jira-password"
export GPG_PASSPHRASE="your-gpg-passphrase"
```

Update settings.xml:
```xml
<server>
  <id>ossrh</id>
  <username>${env.OSSRH_USERNAME}</username>
  <password>${env.OSSRH_PASSWORD}</password>
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
mvn clean deploy -P release

# Or with explicit GPG passphrase
mvn clean deploy -P release -Dgpg.passphrase=your-passphrase
```

### 4. Verify Deployment

After deployment, the artifact will be available on:
- Staging: https://s01.oss.sonatype.org/#stagingRepositories
- Maven Central (after ~2 hours): https://central.sonatype.com/artifact/io.sensorvision/sensorvision-sdk

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
