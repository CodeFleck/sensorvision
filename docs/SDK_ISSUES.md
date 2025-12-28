# SDK Known Issues & Improvements

## Python SDK (indcloud-sdk)

### 🐛 Issue #1: Exception Type Inconsistency (Validation)

**Status:** Identified, needs fix
**Severity:** Medium (API contract violation)
**Version Affected:** v0.1.0

**Description:**

The `Industrial CloudClient.send_data()` method documents that it raises `ValidationError` for invalid inputs (see `indcloud/client.py:95`), but the underlying validation functions in `indcloud/utils.py` actually raise plain `ValueError` instead.

**Evidence:**

```python
# Documented in indcloud/client.py:95
def send_data(self, device_id: str, data: Dict[str, float]) -> IngestionResponse:
    """
    ...
    Raises:
        ValueError: If device_id or data is invalid  # ← Documents ValueError
        ValidationError: If data validation fails    # ← But also mentions ValidationError
        ...
    """
```

```python
# Actual implementation in indcloud/utils.py:95-123
def validate_device_id(device_id: str) -> None:
    if not device_id or not isinstance(device_id, str):
        raise ValueError("Device ID must be a non-empty string")  # ← Raises ValueError
```

**Impact:**

Consumers catching the documented `ValidationError` will miss validation failures:

```python
from indcloud import Industrial CloudClient
from indcloud.exceptions import ValidationError

client = Industrial CloudClient(api_url="...", api_key="...")

try:
    client.send_data("", {"temp": 23.5})  # Invalid empty device ID
except ValidationError as e:
    print("Caught validation error")  # ← This will NOT catch the error
except ValueError as e:
    print("Caught value error")  # ← This WILL catch it
```

**Test Coverage:**

A regression test suite (`tests/test_indcloud_sdk.py`) exercises:
- ✅ Happy path (sync and async clients)
- ✅ Rate limiting scenarios
- ✅ Server error retries
- ✅ Malformed payloads
- ✅ Validation helpers
- ✅ Model serialization

All tests pass with current behavior (catching `ValueError`).

**Resolution Options:**

**Option 1: Change validators to raise ValidationError (Recommended)**

```python
# indcloud/utils.py
from indcloud.exceptions import ValidationError

def validate_device_id(device_id: str) -> None:
    if not device_id or not isinstance(device_id, str):
        raise ValidationError("Device ID must be a non-empty string")
```

**Pros:**
- Matches documented API contract
- Provides more specific exception type
- Better for API consumers (can catch ValidationError specifically)

**Cons:**
- Breaking change for existing code catching ValueError
- Requires version bump to v0.1.1

**Option 2: Update documentation to reflect ValueError**

```python
# indcloud/client.py
def send_data(self, device_id: str, data: Dict[str, float]) -> IngestionResponse:
    """
    ...
    Raises:
        ValueError: If device_id or data is invalid
        AuthenticationError: If API key is invalid
        NetworkError: If network request fails
        ...
    """
```

**Pros:**
- No breaking changes
- Matches current implementation

**Cons:**
- Less specific exception type
- ValidationError exception class becomes unused

**Recommendation:**

Implement **Option 1** and release as v0.1.1 patch:

1. Update `indcloud/utils.py` validators to raise `ValidationError`
2. Update tests to catch `ValidationError`
3. Add migration note in CHANGELOG
4. Publish v0.1.1 to PyPI

**Migration Guide for Users:**

```python
# v0.1.0 (current)
try:
    client.send_data(device_id, data)
except ValueError as e:  # Had to catch ValueError
    handle_validation_error(e)

# v0.1.1 (after fix)
try:
    client.send_data(device_id, data)
except ValidationError as e:  # Now catches ValidationError as documented
    handle_validation_error(e)
```

---

## JavaScript/TypeScript SDK (indcloud-sdk-js)

### Status: No known issues

The JavaScript SDK is built and tested. Awaiting npm publication.

---

## Action Items

- [ ] Fix Python SDK exception inconsistency (Option 1)
- [ ] Update test suite to expect ValidationError
- [ ] Add CHANGELOG entry for v0.1.1
- [ ] Rebuild and test Python SDK
- [ ] Publish v0.1.1 to PyPI
- [ ] Update PyPI package page
- [ ] Integrate regression test suite into CI/CD
- [ ] Complete JavaScript SDK npm publication

---

**Last Updated:** 2025-01-15
**Reporter:** Regression test analysis
**Priority:** Medium (should fix before broader adoption)
