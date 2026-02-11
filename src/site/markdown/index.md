<!--
  ╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲
  Sentry Maven Skin Tools
  ჻჻჻჻჻჻
  Copyright 2017 - 2024 Sentry Software
  ჻჻჻჻჻჻
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  ╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱
  -->
# Getting Started

This artifact contains a collection of Velocity tools that are used by the
[Sentry Maven Skin](https://sentrysoftware/github.io/maven-skin).

This artifact must be loaded as a dependency by the Maven Site plugin, as below:

```xml
<plugin>
  <artifactId>maven-site-plugin</artifactId>
  <dependencies>
    <dependency>
      <groupId>${project.groupId}</groupId>
      <artifactId>${project.artifactId}</artifactId>
      <version>${project.version}</version>
    </dependency>
  </dependencies>
</plugin>
```

This allows the Maven Skin and [Velocity-processed pages in a Maven Site](https://maven.apache.org/doxia/doxia-sitetools/doxia-site-renderer/) to invoke the below Velocity tools:

| Tool | Description | Javadoc |
|---|---|---|
| `#[[$configTool]]#` | To manage site configuration with front matter overrides | [ConfigTool](apidocs/org/sentrysoftware/maven/skin/ConfigTool.html) |
| `#[[$htmlTool]]#` | To manipulate HTML documents or fragments | [HtmlTool](apidocs/org/sentrysoftware/maven/skin/HtmlTool.html) |
| `#[[$imageTool]]#` | To manipulate images | [ImageTool](apidocs/org/sentrysoftware/maven/skin/ImageTool.html) |
| `#[[$indexTool]]#` | To create search indexes | [IndexTool](apidocs/org/sentrysoftware/maven/skin/IndexTool.html) |
| `#[[$aiIndexTool]]#` | To create AI-ready Markdown files from HTML documentation | [AIIndexTool](apidocs/org/sentrysoftware/maven/skin/AIIndexTool.html) |

The above tools are designed to be used only in the Velocity template of a [Maven Site Skin](https://maven.apache.org/plugins/maven-site-plugin/examples/creatingskins.html) as in the example below:

#[[
```velocity
#set($bodyElement = $htmlTool.parseContent($bodyContent))
#set($bodyElement = $imageTool.explicitImageSize($bodyElement, "img", ${project.reporting.outputDirectory}, $currentFileName))
<html>
<body>
$bodyElement.html()
</body>
</html>
```
]]#

## ConfigTool Usage

The `ConfigTool` provides unified configuration management, merging site-wide settings from `site.xml` with per-page overrides from Markdown front matter:

#[[
```velocity
<!-- In your Velocity skin template -->
#set($interpolation = $configTool.getValue($site, $headContent, "interpolation", "maven"))
#set($showToc = $configTool.getBooleanValue($site, $headContent, "showToc", true))
#set($tocMaxDepth = $configTool.getIntValue($site, $headContent, "tocMaxDepth", 3))

#if($showToc)
  <!-- Render table of contents with max depth $tocMaxDepth -->
#end
```
]]#

Configuration precedence (highest to lowest):
1. **Front matter** in Markdown files (converted to `<meta>` tags by Doxia)
2. **Site-wide configuration** in `site.xml` under `<custom>` element
3. **Default value** specified in the method call

Example front matter in a Markdown page:

#[[
```markdown
---
interpolation: none
showToc: false
tocMaxDepth: 2
---

# My Page Title

Content goes here...
```
]]#

Example site-wide configuration in `site.xml`:

#[[
```xml
<project>
  <custom>
    <interpolation>maven</interpolation>
    <showToc>true</showToc>
    <tocMaxDepth>3</tocMaxDepth>
  </custom>
</project>
```
]]#

Additionally, we allow the use of these standard Velocity tools in the Velocity-processed pages (e.g. in `src/site/markdown/*.md.vm`):

| Tool | Description | Javadoc |
|---|---|---|
| `#[[$collection]]#` | Tool gathering several collection utilities | [CollectionTool](https://velocity.apache.org/tools/3.1/apidocs/org/apache/velocity/tools/generic/CollectionTool.html) |
| `#[[$json]]#` | Tool for JSON parsing and rendering | [JsonTool](https://velocity.apache.org/tools/3.1/apidocs/org/apache/velocity/tools/generic/JsonTool.html) |
| `#[[$log]]#` | Tool to trigger logs from withing templates | [LogTool](https://velocity.apache.org/tools/3.1/apidocs/org/apache/velocity/tools/generic/LogTool.html) |

Example:

#[[
```velocity
#set( $repoList = $json.fetch("https://api.github.com/orgs/sentrysoftware/repos") )
#if( $repoList && $repoList.size() > 0 )
| Repository | Description |
|------------|-------------|
#foreach ($repo in $repoList.iterator() )
| $repo.name | $!repo.description |
#end
#else
$log.error("Could not fetch repositories")
*No repositories.*
#end
```
]]#
