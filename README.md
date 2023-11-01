# Open Api Tool
### 功能
讀取 open-api，轉換成 excel 文件

### 使用方式
服務啟動後，可進入 swagger 頁面
> http://localhost:8080/swagger-ui/index.html#/

<img src="https://github.com/PinXian53/open-api-tool/blob/main/image/swagger.png" alt="image">

- templateType: 輸出的模板種類
- openApi: 上傳 open api 3.0 的文字檔 (支援 json, yaml 格式)

範例檔案：resources/templates/test_files/open-api.json

<img src="https://github.com/PinXian53/open-api-tool/blob/main/image/open-api.png" alt="image">

執行後，產生的 excel 如下

<img src="https://github.com/PinXian53/open-api-tool/blob/main/image/export.png" alt="image">

### excel 模板調整
excel 模板使用 JXLS 實作，可以自行調整成所需的格式
> https://jxls.sourceforge.net/

模板位置: resources/templates

<img src="https://github.com/PinXian53/open-api-tool/blob/main/image/template.png" alt="image">