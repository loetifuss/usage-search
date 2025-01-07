# Find Usages IntelliJ Plugin

This plugin for IntelliJ IDEA enables searching for Java references in all library code of a project imported using Gradle.
By default IntelliJ only searches in code that is locally available as source code.

See also this issue on Jetbrains YouTrack: https://youtrack.jetbrains.com/issue/IDEA-358774/Find-Usages-doesnt-work-for-classes-used-in-library-code-with-the-attached-sources

# Usage

The plugin adds an additional context action "Find external usages" next to the existing "Find usages".
"Find external usages" will find all references of methods/classes/fields in the project's libraries while "Find usages" finds only references in source code.

## Assumptions made

The plugin currently assumes the following:

- Source code is downloaded for all libraries (use "Download Sources" in Gradle View)
- Compiled classes are present in `build/classes/java` for all .java Files
