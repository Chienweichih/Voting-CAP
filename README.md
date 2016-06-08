# Implementing Real-time POV for Cloud Storage by Replication and Voting
Author: Wei-Chih Chien

(This README reference from [Malthael's CAP-for-SOA-Systems README.md](https://github.com/CloudComLab/CAP-for-SOA-Systems/blob/master/README.md))

## 如何開始
1. 使用 [GitHub Desktop](https://desktop.github.com) Clone 本專案
2. 開啟 [NetBeans IDE](https://netbeans.org) 上方選單選擇 `File` > `Open Project` > `本專案路徑`
3. 檢查基本設定 (都在 `wei_chih.service.Config` 中)：
    - `SERVICE_HOSTNAME` 的值是否為 `"localhost"`
    - `CLIENT_ACCOUNT_PATH` 設定為要處理的檔案資料夾路徑 (Accounts)
4. 上方選單選擇 `Run` > `Clean and Build Project` 編譯 (快捷鍵為 Shift-F11)
5. 新增伺服器與使用者的執行設定：
    1. 上方選單選擇 `File` > `Project Properties`
    2. 選擇 `Categories` 中的 `Run` 標籤
    3. 選擇 `New` > 輸入 `client` 或 `server`
    4. 首先設定 `Main Class`：
        * `client` 為 `wei_chih.client.Experiment`
        * `server` 為 `wei_chih.service.SocketServer`
    5. 最後`Arguments` 設定成 `A` 或 `B` 或 `C` (分別是 `Account A`, `Account B` 和 `Account C`)
6. 執行 server：
    1. 上方選單選擇 `Run` > `Set Project Configuration` > `server` (或是 Toorbars 中的選單)
    2. 上方選單選擇 `Run` > `Run Project` 啟動伺服器 (快捷鍵為 F6)
    3. 等待 console 出現 `Ready to go!` 字樣
7. 執行 client：
    1. 上方選單選擇 `Run` > `Set Project Configuration` > `client` (或是 Toorbars 中的選單)
    2. 上方選單選擇 `Run` > `Run Project` 模擬使用者行為 (快捷鍵為 F6)
    3. 待 `wei_chih.client.Experiments.java` 結束之後，記得也要關閉伺服器

**請使用 JDK 8 以上版本**

## 設定遠端 server
若要將 server 執行於遠端的 vm ，請依以下步驟設定：

1. 新增遠端 Java 平台：
    1. 上方選單選擇 `Tools` > `Java Platforms`
    2. 點選 `Add Platform...`
    3. 選擇 `Remote Java Standard Edition`，點選 `Next`
    3. 將遠端 server 的 detail 填入，點選 `Finish` 就完成了
2. 更改伺服器的執行設定：
    1. 上方選單選擇 `File` > `Project Properties`
    2. 選擇 `Categories` 中的 `Run` 標籤
    3. `Configuartion` 選擇 `server`
    4. `Runtime Platform` 選擇剛剛新增的遠端 Java 平台，點選 `ok` 完成
3. 更改基本設定 (都在 `wei_chih.service.Config` 中)：
    - `SERVICE_HOSTNAME` 的值為 `"遠端 server ip"`
    - `SERVER_ACCOUNT_PATH` 改為遠端 server上的檔案資料夾路徑 (Accounts)
4. 上方選單選擇 `Run` > `Clean and Build Project` 編譯 (快捷鍵為 Shift-F11)
5. 確認伺服器端與使用者端 `keypairs` 資料夾中的簽章檔案相同，若不相同執行時會丟出 `java.security.SignatureException`
6. 依照前面 `如何開始` 的方法執行 server 和 client 即可

## 介紹

這個 Project 是繼承 Malthael 寫的 [Cryptographic Accountability Protocol for Service-Oriented Architecture Systems](https://github.com/CloudComLab/CAP-for-SOA-Systems)，所有跟我的論文有關的 code 都在 `src/wei_chih/` 目錄底下，包含三種 CAP 的實作：
* Non-CAP (Non-POV)
* Voting-POV
* WeiShian-POV (2014 Cloud Com)

系統主要有四大部分：

* 服務提供者（service provider）
* 使用者（client）
* 中間傳遞的訊息（message）
* 雜湊樹 (merkle tree)

### 服務提供者（service provider）

簡單來說就是伺服器，啟動的時候會開啟以下`SocketServer`，監聽不同的 port：
1. NonPOVHandler
2. VotingHandler (由 `VotingSyncServer.SERVER_PORTS` 決定數量)
3. VotingSyncServer
4. WeiShianHandler
5. WeiShianSyncServer

每個請求抵達之後，會產生一個 `Thread` 並配合不同 CAP 自己的 handler 去執行請求。你可以在 `wei_chih.service.handler` 找到五個 handler，並且透過裡頭的 `public void run()` 觀察其行為。

### 使用者（client）

使用者的實作都在 `wei_chih.client` 中，其中的 `Experiments.java` 統整了各 CAP 使用者的呼叫，執行他就可以直接比較三者的差異。

使用者主要透過 `public void run(final List<Operation> operations, int runTimes)` 執行，會輪流呼叫所有動作：

```
for (int i = 0; i < runTimes; i++) {
  execute(operations.get(i % operations.size()));
}
```

`execute(Operation op)` 會先去與伺服器建立連接，然後再執行 `protected void hook(Operation op, Socket socket, DataOutputStream out, DataInputStream in)`。你可以去觀察每個使用者裡面的此方法，並與對應到的 handler 搭配著看，就可以得知每個 CAP 的運作模式。

### 中間傳遞的訊息（message）

以 [SOAP](https://en.wikipedia.org/wiki/SOAP) 格式為主，使用 `javax.xml.soap.MessageFactory` 產生，另外還附有電子簽章，全部實作可以在 `wei_chih.message` 找到。

`Non CAP (Non POV)`, `Voting`, `Wei-shian` 提供：

* Request
* Acknowledgement

### 雜湊樹 (merkle tree)
雜湊樹的實作在 `wei_chih.utility.MerkleTree.java` 中，每個節點的資料用 inner class `Node`儲存。首先呼叫建構元 `public MerkleTree(File rootPath)` 建立一個雜湊樹，接者可以用以下 member function 操作雜湊樹物件：

- 拷貝整棵雜湊樹：  `public MerkleTree(MerkleTree merkleTree)`
- 更新一個節點的雜湊值： `public void update(String fname, String digest)`
- 刪除一個節點： `public void delete(String fname)`
- 取得雜湊樹的根節點雜湊值： `public String getRootHash()`
- 取得新一個節點的雜湊值：`public String getDigest(String path)`
- 從已經取 hash 的檔案推算出根節點雜湊值的實驗： `private static String getRoothashFromHashedFiles(Node rootNode)`


## License

   Copyright (c) 2016 Cloud Computing Laboratory

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.