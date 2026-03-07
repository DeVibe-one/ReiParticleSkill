# Third-Party Notices

This file lists the third-party dependencies used by this repository and the notices worth keeping for review or redistribution.

---

## ClassGraph

- **Artifact:** `io.github.classgraph:classgraph:4.8.174`
- **License:** MIT
- **Homepage:** https://github.com/classgraph/classgraph
- **Used by:** Classpath scanning in the bundled `ReiParticlesAPI` runtime

MIT License

Copyright (c) 2019 Luke Hutchison

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

---

## Minecraft Forge

- **Artifact:** `net.minecraftforge:forge:1.20.1-47.2.0`
- **License:** LGPL-2.1-only (Forge), Minecraft EULA (Mojang)
- **Homepage:** https://minecraftforge.net
- **Note:** Forge is the modding platform used to build this project. It is not bundled into the output jar. Minecraft remains proprietary software owned by Mojang Studios / Microsoft.

---

## JUnit 5

- **Artifact:** `org.junit.jupiter:junit-jupiter` (test only)
- **License:** EPL-2.0
- **Homepage:** https://junit.org/junit5/
- **Note:** Test-scoped only. It is not included in release jars.

---

This file is informational and does not constitute legal advice.

If a third-party dependency is shaded or otherwise bundled into a release jar in the future, add its license and notice text here and include it in `META-INF/`.
