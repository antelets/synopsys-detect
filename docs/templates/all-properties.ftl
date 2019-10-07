This page lists all detect properties including deprecated and advanced, for most use cases see [basic properties](../basic-properties).

<#list groups as group>

[${group.groupName}](../${group.groupName}/)

| Property | Description |
| --- | --- |
<#list group.simple![] as option>
| [${option.propertyKey}](../${group.groupName}/<#if option.propertyName?has_content>#${option.propertyName?replace(" ", "-")}</#if>) | <#if option.defaultValue??>default: ${option.defaultValue} <br /><br /> </#if><#if option.hasAcceptableValues> Acceptable Values: ${option.acceptableValues?join(", ")} <br /><br /></#if><#if option.propertyName?has_content>${option.propertyName}: </#if>${option.description} <br /><br /> <#if option.deprecated>**DEPRECATED: ${option.deprecatedDescription!"This property is deprecated."}**</#if> |
</#list>
<#list group.advanced![] as option>
| [${option.propertyKey}](../${group.groupName}/<#if option.propertyName?has_content>#${option.propertyName?replace(" ", "-")}</#if>) <br /> (Advanced) | <#if option.defaultValue??>default: ${option.defaultValue} <br /><br /> </#if><#if option.hasAcceptableValues> Acceptable Values: ${option.acceptableValues?join(", ")} <br /><br /></#if><#if option.propertyName?has_content>${option.propertyName}: </#if>${option.description} <br /><br /> <#if option.deprecated>**DEPRECATED: ${option.deprecatedDescription!"This property is deprecated."}**</#if> |
</#list>
<#list group.deprecated![] as option>
| [${option.propertyKey}](../${group.groupName}/<#if option.propertyName?has_content>#${option.propertyName?replace(" ", "-")}</#if>) <br /> (Deprecated)| <#if option.defaultValue??>default: ${option.defaultValue} <br /><br /> </#if><#if option.hasAcceptableValues> Acceptable Values: ${option.acceptableValues?join(", ")} <br /><br /></#if><#if option.propertyName?has_content>${option.propertyName}: </#if>${option.description} <br /><br /> <#if option.deprecated>**DEPRECATED: ${option.deprecatedDescription!"This property is deprecated."}**</#if> |
</#list>

</#list>