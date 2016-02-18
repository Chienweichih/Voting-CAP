package wei_chih.utility;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;
import wei_chih.service.Config;

/**
 *
 * @author Chienweichih
 */
public class MerkleTree implements Serializable {
    private static class Node implements Serializable {
        private final String fname;
        private String digest;
        private final Node parent;
        private List<Node> children;

        private Node(String fname, String digest, Node parent, List<Node> children) {
            this.fname = fname;
            this.digest = digest;
            this.parent = parent;
            this.children = children;
        }
        
        private Node(Node node, Node parent) {
            this.fname = node.fname;
            this.digest = node.digest;
            this.parent = parent;
            this.children = null;

            if (node.isDirectory()) {
                this.children = new ArrayList<>();
                for (Node n : node.children) {
                    this.children.add(new Node(n, this));
                }
            }
        }
                   
        private boolean isDirectory() {
            return children != null;
        }
        
        private static Node getNode(String path, Node root) {
            String pattern = Pattern.quote("/");
            String[] splittedFileName = path.split(pattern);

            Node target = root;
            if (splittedFileName.length <= 1) {
                return target;
            }

            for (String token : Arrays.copyOfRange(splittedFileName, 1, splittedFileName.length)) {
                int index = 0;
                for (Node node : target.children) {
                    if (node.fname.equals(token)) {
                        break;
                    }
                    ++index;
                }
                target = target.children.get(index);
            }
            return target;
        }
    }
    
    private final Node root;
    
    public MerkleTree(MerkleTree merkleTree) {
        this.root = new Node(merkleTree.root, null);
    }
    
    public MerkleTree(File rootPath) {
        this.root = create(rootPath, null);
    }
    
    private Node create(File file, Node parent) {
        Node node = new Node(file.getName(), null, parent, null);
                
        if (file.isFile()) {
            node.digest = Utils.digest(file);
        } else {
            node.children = new ArrayList<>();
            String folderDigest = "";
            
            for (File f : sortedFiles(file.listFiles())) {
                Node newNode = create(f, node);
                node.children.add(newNode);
                folderDigest += newNode.digest;
            }
            node.digest = Utils.digest(Utils.Str2Hex(folderDigest));
        }
        
        return node;
    }
    
    public void update(String fname, String digest) {
        update(Node.getNode(fname, root), digest);
    }
    
    private void update(Node node, String digest) {
        node.digest = digest;
        
        while (node.parent != null) {
            node = node.parent;
            String newDigest = "";
            
            for (Node n : node.children) {
                newDigest += n.digest;
            }
            node.digest = Utils.digest(Utils.Str2Hex(newDigest));
        }
    }
    
    public void delete(String fname) {
        Node node = Node.getNode(fname, root);
        fname = node.fname;
        node = node.parent;
        
        int index = 0;
        for (Node n : node.children) {
            if (n.fname.equals(fname)) {
                break;
            }
            ++index;
        }
        node.children.remove(index);
        update(node.children.get(0), node.children.get(0).digest);
    }
    
    public String getRootHash() {
        return root.digest;
    }
    
    public String getDigest(String path) {
        return Node.getNode(path, root).digest;
    }
    
    private static List<File> sortedFiles(File[] unSortedFiles) {
        if (unSortedFiles == null) {
            System.err.println("exceptionininitializererror?");
        }
        List<File> files = Arrays.asList(unSortedFiles);
        Collections.sort(files, (File lhs, File rhs) -> {
            return lhs.getName().compareTo(rhs.getName());
        });
        return files;
    }
    
    private void print() {
        Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            System.out.println(node.fname + " " + node.digest);
            if (node.isDirectory()) {
                for (Node n : node.children) {
                    queue.add(n);
                }
            }
        }
    }
    
    public static void main(String[] args) {
        
        long time;
        
        for(int i = 0; i < 3; ++i) {
            time = System.currentTimeMillis();

            String dataDirPath;
            if (args.length != 1) {
                dataDirPath = Config.DATA_A_PATH;
            } else {
                dataDirPath = Utils.getDataDirPath(args[0]);
            }

            String hashValue = new MerkleTree(new File(dataDirPath)).getRootHash();
            System.out.println("RootHash Hash Value: " + hashValue);

            time = System.currentTimeMillis() - time;
            System.out.println("Generate Root Hash Cost: " + time/1000.0 + "s");
        }
    }
}
