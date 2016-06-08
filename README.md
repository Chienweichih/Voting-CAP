# Implementing Real-time POV for Cloud Storage by Replication and Voting
Author: Wei-Chih Chien

(This README reference from [Malthael's CAP-for-SOA-Systems README.md](https://github.com/CloudComLab/CAP-for-SOA-Systems/blob/master/README.md))

## �p��}�l
1. �ϥ� [GitHub Desktop](https://desktop.github.com) Clone ���M��
2. �}�� [NetBeans IDE](https://netbeans.org) �W������ `File` > `Open Project` > `���M�׸��|`
3. �ˬd�򥻳]�w (���b `wei_chih.service.Config` ��)�G
    - `SERVICE_HOSTNAME` ���ȬO�_�� `"localhost"`
    - `CLIENT_ACCOUNT_PATH` �]�w���n�B�z���ɮ׸�Ƨ����| (Accounts)
4. �W������ `Run` > `Clean and Build Project` �sĶ (�ֱ��䬰 Shift-F11)
5. �s�W���A���P�ϥΪ̪�����]�w�G
    1. �W������ `File` > `Project Properties`
    2. ��� `Categories` ���� `Run` ����
    3. ��� `New` > ��J `client` �� `server`
    4. �����]�w `Main Class`�G
        * `client` �� `wei_chih.client.Experiment`
        * `server` �� `wei_chih.service.SocketServer`
    5. �̫�`Arguments` �]�w�� `A` �� `B` �� `C` (���O�O `Account A`, `Account B` �M `Account C`)
6. ���� server�G
    1. �W������ `Run` > `Set Project Configuration` > `server` (�άO Toorbars �������)
    2. �W������ `Run` > `Run Project` �Ұʦ��A�� (�ֱ��䬰 F6)
    3. ���� console �X�{ `Ready to go!` �r��
7. ���� client�G
    1. �W������ `Run` > `Set Project Configuration` > `client` (�άO Toorbars �������)
    2. �W������ `Run` > `Run Project` �����ϥΪ̦欰 (�ֱ��䬰 F6)
    3. �� `wei_chih.client.Experiments.java` ��������A�O�o�]�n�������A��

**�Шϥ� JDK 8 �H�W����**

## �]�w���� server
�Y�n�N server ����󻷺ݪ� vm �A�Ш̥H�U�B�J�]�w�G

1. �s�W���� Java ���x�G
    1. �W������ `Tools` > `Java Platforms`
    2. �I�� `Add Platform...`
    3. ��� `Remote Java Standard Edition`�A�I�� `Next`
    3. �N���� server �� detail ��J�A�I�� `Finish` �N�����F
2. �����A��������]�w�G
    1. �W������ `File` > `Project Properties`
    2. ��� `Categories` ���� `Run` ����
    3. `Configuartion` ��� `server`
    4. `Runtime Platform` ��ܭ��s�W������ Java ���x�A�I�� `ok` ����
3. ���򥻳]�w (���b `wei_chih.service.Config` ��)�G
    - `SERVICE_HOSTNAME` ���Ȭ� `"���� server ip"`
    - `SERVER_ACCOUNT_PATH` �אּ���� server�W���ɮ׸�Ƨ����| (Accounts)
4. �W������ `Run` > `Clean and Build Project` �sĶ (�ֱ��䬰 Shift-F11)
5. �T�{���A���ݻP�ϥΪ̺� `keypairs` ��Ƨ�����ñ���ɮ׬ۦP�A�Y���ۦP����ɷ|��X `java.security.SignatureException`
6. �̷ӫe�� `�p��}�l` ����k���� server �M client �Y�i

## ����

�o�� Project �O�~�� Malthael �g�� [Cryptographic Accountability Protocol for Service-Oriented Architecture Systems](https://github.com/CloudComLab/CAP-for-SOA-Systems)�A�Ҧ���ڪ��פ妳���� code ���b `src/wei_chih/` �ؿ����U�A�]�t�T�� CAP ����@�G
* Non-CAP (Non-POV)
* Voting-POV
* WeiShian-POV (2014 Cloud Com)

�t�ΥD�n���|�j�����G

* �A�ȴ��Ѫ̡]service provider�^
* �ϥΪ̡]client�^
* �����ǻ����T���]message�^
* ����� (merkle tree)

### �A�ȴ��Ѫ̡]service provider�^

²��ӻ��N�O���A���A�Ұʪ��ɭԷ|�}�ҥH�U`SocketServer`�A��ť���P�� port�G
1. NonPOVHandler
2. VotingHandler (�� `VotingSyncServer.SERVER_PORTS` �M�w�ƶq)
3. VotingSyncServer
4. WeiShianHandler
5. WeiShianSyncServer

�C�ӽШD��F����A�|���ͤ@�� `Thread` �ðt�X���P CAP �ۤv�� handler �h����ШD�C�A�i�H�b `wei_chih.service.handler` ��줭�� handler�A�åB�z�L���Y�� `public void run()` �[���欰�C

### �ϥΪ̡]client�^

�ϥΪ̪���@���b `wei_chih.client` ���A�䤤�� `Experiments.java` �ξ�F�U CAP �ϥΪ̪��I�s�A����L�N�i�H��������T�̪��t���C

�ϥΪ̥D�n�z�L `public void run(final List<Operation> operations, int runTimes)` ����A�|���y�I�s�Ҧ��ʧ@�G

```
for (int i = 0; i < runTimes; i++) {
  execute(operations.get(i % operations.size()));
}
```

`execute(Operation op)` �|���h�P���A���إ߳s���A�M��A���� `protected void hook(Operation op, Socket socket, DataOutputStream out, DataInputStream in)`�C�A�i�H�h�[��C�ӨϥΪ̸̭�������k�A�ûP�����쪺 handler �f�t�۬ݡA�N�i�H�o���C�� CAP ���B�@�Ҧ��C

### �����ǻ����T���]message�^

�H [SOAP](https://en.wikipedia.org/wiki/SOAP) �榡���D�A�ϥ� `javax.xml.soap.MessageFactory` ���͡A�t�~�٪����q�lñ���A������@�i�H�b `wei_chih.message` ���C

`Non CAP (Non POV)`, `Voting`, `Wei-shian` ���ѡG

* Request
* Acknowledgement

### ����� (merkle tree)
����𪺹�@�b `wei_chih.utility.MerkleTree.java` ���A�C�Ӹ`�I����ƥ� inner class `Node`�x�s�C�����I�s�غc�� `public MerkleTree(File rootPath)` �إߤ@�������A���̥i�H�ΥH�U member function �ާ@����𪫥�G

- ������������G  `public MerkleTree(MerkleTree merkleTree)`
- ��s�@�Ӹ`�I������ȡG `public void update(String fname, String digest)`
- �R���@�Ӹ`�I�G `public void delete(String fname)`
- ���o����𪺮ڸ`�I����ȡG `public String getRootHash()`
- ���o�s�@�Ӹ`�I������ȡG`public String getDigest(String path)`
- �q�w�g�� hash ���ɮױ���X�ڸ`�I����Ȫ�����G `private static String getRoothashFromHashedFiles(Node rootNode)`


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