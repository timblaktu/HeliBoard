# CI for Feature Branches

## Purpose
Enable GitHub Actions workflows to run on feature branch pushes, allowing CI testing before PR submission.

## Context
The upstream HeliBoard repository has CI workflows configured to run only on:
- **build-debug-apk.yml**: Only on `workflow_dispatch` (manual trigger)
- **build-test-auto.yml**: Only on `pull_request` for Java changes

This means CI does NOT automatically run when pushing to feature branches, which makes it difficult to test changes before creating a PR.

## Changes Made
Modified both workflow files to trigger on `feature/**` branch pushes:

### build-debug-apk.yml
```yaml
on:
  push:
    branches:
      - main
      - 'feature/**'  # Added: Run on feature branches
  pull_request:
    branches:
      - main
  workflow_dispatch:
```

### build-test-auto.yml
```yaml
on:
  push:
    branches:
      - 'feature/**'  # Added: Run on feature branches
    paths:
      - 'app/**'
  pull_request:
    paths:
      - 'app/src/main/java**'
  workflow_dispatch:
```

## Benefits
1. **Pre-PR Testing**: Test changes in CI before creating a pull request
2. **Faster Iteration**: Catch build/test failures early
3. **Fork-specific**: These changes only affect your fork, not upstream
4. **Feature Branch Isolation**: Each feature branch gets its own CI runs

## Usage
After pushing this branch to your fork:
1. Any push to a `feature/*` branch will trigger CI
2. Check the Actions tab on GitHub to see build status
3. Fix any issues before creating PRs to upstream

## Note
This is a development convenience feature for your fork. It should not be included in PRs to upstream unless specifically requested by maintainers.