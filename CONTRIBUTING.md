Contributing
====

If you would like to contribute to the development of JavaPhoenixChannels, please submit an issue or open a pull request. Contributions of _any kind_ are always welcome:

- Features
- Bug Fixes
- Tests
- Documentation
- Examples

For any contribution, there are a few guidelines that need to be followed.

# Submitting an Issue

- Use GitHub issue search to determine if the issue has already been reported.
- Submit the issue to the JavaPhoenixChannels Issues on GitHub.
- Always be as helpful, inclusive and friendly as possible in the words you use. Demanding or offensive issues are very likely to be closed.
- Clearly describe the steps to reproduce the issue.
- Indicate the version where the issue can be reproduced.

# Making Changes
- Fork the JavaPhoenixChannels repository on GitHub.
- Ensure an issue exists for the change you are making.
- Create a branch for your fix: (`git checkout -b fix/master/my_feature_or_fix master`).
- Make changes and submit a pull request for your branch in your fork.
- One or more of the repository maintainers will review your change and provide feedback and/or merge your pull request.
- *By submitting a change*, you agree that your work will be licensed under the license used by the project.
- *Never* merge `master` into your branch, always `rebase`.

# Building the Library
Using Gradle 2.2.1 or later:

```shell
gradle build
```

# Release and Publication
In order to release, you require full contributor rights and a Bintray account with write access.

```
export BINTRAY_USER=...
export BINTRAY_KEY=...
gradle bintrayUpload
```

Tag the new version:
```
git tag -a v0.2.0
git push origin v0.2.0
```

At this point, you have to go to the Bintray web interface and click 'publish' for the new version.

