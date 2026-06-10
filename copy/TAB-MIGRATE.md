# TAB 迁移说明

把本目录下的 `plugins/` 复制到服务器根目录，最终服务器上应有：

```text
plugins/TAB-6.0.3-Vanilla.jar
plugins/TAB/
```

迁移前先停服。迁移后启动服务端，控制台应看到 `TAB` 插件加载；进服后按 Tab 检查顶部标题是否为 `YuHua服务器`。

注意：保持 CMI 的 tablist 模块关闭，避免 CMI 和 TAB 同时接管玩家列表。

TAB jar SHA-256：

```text
57adc8a655736a50a79dbe11a67a8a7542df20db8dd76650ec934c9e0e2a926c  plugins/TAB-6.0.3-Vanilla.jar
```
