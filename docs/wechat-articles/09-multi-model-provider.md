# 多模型接入的现实问题：OpenAI 兼容协议背后的工程取舍

## 适合标题

- AI 应用为什么不能只绑定一个模型？
- OpenAI 兼容协议背后，真正麻烦的是能力差异
- 悟空 AI CRM 的多模型接入设计

## 导语

做 AI 应用时，很多人第一版会直接写死一个模型地址和 API Key。这样可以快速跑通 Demo，但进入真实部署后，很快就会遇到问题：有的用户用 OpenAI，有的用户用通义千问，有的用户用 Kimi、DeepSeek、豆包、混元、智谱，还有人要接自建 OpenAI 兼容服务。

AI CRM 如果要开源可部署，就不能把模型供应商写死。

## 正文

悟空 AI CRM 的后端有一个 `AiProviderRegistry`，里面注册了多个 AI 服务商描述，包括 OpenAI、阿里云百炼 / 通义千问、Moonshot / Kimi、DeepSeek、火山方舟 / 豆包、腾讯混元、MiniMax、智谱、悟空云 AI、自定义 OpenAI 兼容服务等。

每个 provider 不只是一个名称，还包含 baseUrl、补全路径、Embedding 路径、推荐模型、模型填写提示、额外请求头提示、默认能力、URL 识别关键词、工具调用开关关键词、视觉能力关键词等。

这个设计说明一个现实问题：OpenAI 兼容协议只是接口层面的兼容，不代表能力完全一致。

有的模型支持工具调用，有的不支持；有的支持视觉输入，有的不支持；有的路径是 `/chat/completions`，有的需要额外兼容前缀；有的供应商需要额外请求头；有的模型虽然能对话，但不适合做 CRM 主模型，因为工具调用能力不稳定。

`AiProviderDescriptor.resolveCapabilities` 会根据模型名称和 provider 默认配置推断能力，比如是否支持流式输出、工具调用、视觉能力、音频转写等。`DynamicChatClientProvider` 则负责读取系统配置，解析运行时模型配置，并创建带工具组的 `ChatClient`。

这对私有化部署很重要。开源项目的用户环境差异非常大：有人有官方 OpenAI Key，有人只能使用国内云厂商模型，有人部署在内网，需要通过自定义兼容服务转发。系统必须允许他们在后台切换 provider、baseUrl、model、temperature、maxTokens 和 extraHeaders。

多模型接入还影响业务体验。比如用户上传图片附件时，如果当前模型不支持视觉，系统会给出提示：当前模型不支持图片直传，请仅基于可提取文本回答；如需图片理解，请切换支持视觉的模型。这种能力检测能避免用户误以为 AI 已经“看懂图片”。

对 AI CRM 来说，模型不是越强越好，也不是越便宜越好。最合适的模型要看业务场景：客户查询和字段更新需要稳定工具调用；知识库问答需要长文本和引用质量；简历解析需要结构化抽取能力；图片、录音、附件处理又需要多模态能力。

## 工程启发

多模型不是简单做一个下拉框，而是要抽象 provider、模型能力、请求路径、额外 header、工具调用能力和降级提示。否则用户虽然能配置模型，但业务链路不一定可靠。

## 代码观察点

- `AiProviderRegistry` 注册多个 OpenAI 兼容服务商。
- `AiProviderDescriptor` 描述 provider 能力和模型提示。
- `DynamicChatClientProvider` 根据数据库配置动态创建 `ChatClient`。
- `AiModelCapabilities` 控制工具调用、流式输出和视觉能力。

## 结尾

AI 应用真正开源后，模型接入一定会面对复杂环境。悟空 AI CRM 的多 provider 设计，解决的不是“换一个 baseUrl”这么简单，而是让业务系统可以在不同模型能力之间做可控切换。

