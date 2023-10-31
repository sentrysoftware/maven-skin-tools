# Getting Started

This artifact contains a bunch of Velocity tools that are used by the
[Sentry Maven Skin](https://sentrysoftware/github.io/maven-skin).

This artifact must be loaded as a dependency to the Maven Site plugin, as below:

```xml
<plugin>
  <artifactId>maven-site-plugin</artifactId>
  <dependencies>
    <dependency>
      <groupId>org.sentrysoftware.maven</groupId>
      <artifactId>maven-skin-tools</artifactId>
      <version>1.1.00</version>
    </dependency>
  </dependencies>
</plugin>
```

This allows the Maven Skin to invoke the tools declared in `tools.xml`:

| Tool | Description | Javadoc |
|---|---|---|
| `htmlTool` | To manipulate HTML documents or fragments | [HtmlTool](apidocs/org/sentrysoftware/maven/skin/HtmlTool.html) |
| `imageTool` | To manipulate images | [ImageTool](apidocs/org/sentrysoftware/maven/skin/ImageTool.html) |
| `indexTool` | To create search indexes | [IndexTool](apidocs/org/sentrysoftware/maven/skin/IndexTool.html) |

These tools can be invoked in the Velocity templates as in the example below:

```html
#set($bodyElement = $htmlTool.parseContent($bodyContent))
#set($bodyElement = $imageTool.explicitImageSize($bodyElement, "img", ${project.reporting.outputDirectory}, $currentFileName))
<html>
<body>
$bodyElement.html()
</body>
</html>
```
