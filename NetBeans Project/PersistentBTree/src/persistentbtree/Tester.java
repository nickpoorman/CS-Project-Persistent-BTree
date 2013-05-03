package persistentbtree;

import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import persistentbtree.BTree;

public class Tester {

    public Tester() {
        //   this.printDifferencesCSAndCTS();
        //   this.printDifferencesCSAndMoxie();
        //  this.printMismatchedUIDandGECOSonCTAandMoxie();
    }

    public void loadAccounts() {
        try {
            File tmp = new File("moxieBTree");
            File tmp2 = new File("csBTree");
            File tmp3 = new File("ctsBTree");
            RandomAccessFile file = new RandomAccessFile(tmp, "rw");
            RandomAccessFile file2 = new RandomAccessFile(tmp2, "rw");
            RandomAccessFile file3 = new RandomAccessFile(tmp3, "rw");
            BTree moxieBTree = new BTree(file);
            BTree csBTree = new BTree(file2);
            BTree ctsBTree = new BTree(file3);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*
     * Prints out local accounts on Moxie that are neither in CS nor CTS.
     */
    public void printMoxieAccountsNotCsNorCTS() {
        //create and start the loading spinner so we
        // know the program didn't crash while it loads the tree's
        Loading loadingDisplay = new Loading();
        Thread t = new Thread(loadingDisplay);
        t.start();


        List<String[]> cs;
        List<String[]> cts;
        List<String[]> moxie;

        try {
            File tmp = new File("MoxieAccountsNotCsNorCTS.btree");
            RandomAccessFile file = new RandomAccessFile(tmp, "rw");
            BTree moxieTree = new BTree(file);
            cs = nisEntries("nis://altair.cs.oswego.edu/cs.oswego.edu");
            cts = nisEntries("nis://natasha.oswego.edu/ACAD");
            moxie = passwdEntries("/etc/passwd");
            for (String[] s : moxie) {
                //UID = s[2]     GECOS = s[4]
                moxieTree.add("" + s[2], "t");
            }

            //for each tree
            //remove whats in there from other tree
            for (String[] s : cts) {
                //UID = s[2]     GECOS = s[4]
                moxieTree.remove(s[2]);
            }
            for (String[] s : cs) {
                //UID = s[2]     GECOS = s[4]
                moxieTree.remove(s[2]);
            }
            loadingDisplay.doneLoading();
            while (t.isAlive()) {
            }

            System.out.println("Accounts on Moxie that are not on CTS nor on CS: \n" + moxieTree.traverseKeys(moxieTree.root));

        } catch (Exception e) {
            loadingDisplay.doneLoading();
            e.printStackTrace();
        }
    }

    /*
     * Prints out mismatches in uid or gecos fields of local vs those on CS or CTS
     */
    public void printMismatchedUIDandGECOSonCTAandMoxie() {
        Loading loadingDisplay = new Loading();
        Thread t = new Thread(loadingDisplay);
        t.start();



        List<String> mismatches = new LinkedList();
        List<String[]> cs;
        List<String[]> cts;
        List<String[]> moxie;

        //load the tree
        try {
            File tmp = new File("MismatchedUIDandGECOSonCTAandMoxiecsTree.btree");
            File tmp2 = new File("MismatchedUIDandGECOSonCTAandMoxiectsTree.btree");

            RandomAccessFile file = new RandomAccessFile(tmp, "rw");
            RandomAccessFile file2 = new RandomAccessFile(tmp2, "rw");
            BTree csTree = new BTree(file);
            BTree ctsTree = new BTree(file2);
            cs = nisEntries("nis://altair.cs.oswego.edu/cs.oswego.edu");
            for (String[] s : cs) {
                //UID = s[2]     GECOS = s[4]
                csTree.add(s[2], s[4]);
            }
            cts = nisEntries("nis://natasha.oswego.edu/ACAD");
            for (String[] s : cts) {
                //UID = s[2]     GECOS = s[4]
                ctsTree.add(s[2], s[4]);
            }

            moxie = passwdEntries("/etc/passwd");

            //for each tree
            //check other tree for UID and GECOS of local
            for (String[] s : moxie) {
                String moxieValue = s[4];
                moxieValue = moxieValue.trim();
                //UID = s[2]     GECOS = s[4]
                if (csTree.contains(s[2])) {
                    String csTreeValue = csTree.getValue(s[2]);
                    csTreeValue = csTreeValue.trim();
                    if (!csTreeValue.equals(moxieValue)) {
                        mismatches.add("Account: " + s[2] + " on Moxie does not match account: " + s[2] + " on CS. " + moxieValue + "/" + csTreeValue);
                    }
                }

                if (ctsTree.contains(s[2])) {
                    String ctsTreeValue = ctsTree.getValue(s[2]);
                    ctsTreeValue = ctsTreeValue.trim();
                    if (!ctsTreeValue.equals(moxieValue)) {
                        mismatches.add("Account: " + s[2] + " on Moxie does not match account: " + s[2] + " on CTS. " + moxieValue + "/" + ctsTreeValue);
                    }
                }
            }

            loadingDisplay.doneLoading();
            while (t.isAlive()) {
            }

            for (String s : mismatches) {
                System.out.println(s);
            }


        } catch (Exception e) {
            loadingDisplay.doneLoading();
            e.printStackTrace();
        }
    }
    /*
     * Print any accounts that are on CS and not CTS & viceversa
     */

    public void printDifferencesCSAndCTS() {
        //create and start the loading spinner so we
        // know the program didn't crash while it loads the tree's
        Loading loadingDisplay = new Loading();
        Thread t = new Thread(loadingDisplay);
        t.start();



        try {
            File tmp = new File("DifferencesCSAndCTScsTree.btree");
            File tmp2 = new File("DifferencesCSAndCTSctsTree.btree");

            RandomAccessFile file = new RandomAccessFile(tmp, "rw");
            RandomAccessFile file2 = new RandomAccessFile(tmp2, "rw");
            BTree csTree = new BTree(file);
            BTree ctsTree = new BTree(file2);
            List<String[]> cs =
                    nisEntries("nis://altair.cs.oswego.edu/cs.oswego.edu");
            for (String[] s : cs) {
                //UID = s[2]     GECOS = s[4]
                csTree.add(s[2], s[4]);
            }
            List<String[]> cts =
                    nisEntries("nis://natasha.oswego.edu/ACAD");
            for (String[] s : cts) {
                //UID = s[2]     GECOS = s[4]
                ctsTree.add(s[2], s[4]);
            }

            //for each tree
            //remove whats in there from other tree
            for (String[] s : cts) {
                //UID = s[2]     GECOS = s[4]
                csTree.remove(s[2]);
            }
            for (String[] s : cs) {
                //UID = s[2]     GECOS = s[4]
                ctsTree.remove(s[2]);
            }
            loadingDisplay.doneLoading();
            while (t.isAlive()) {
            }

            System.out.println("Accounts on CS not on CTS: \n" + csTree.traverseKeys(csTree.root));
            System.out.println("Accounts on CTS not on CS: \n" + ctsTree.traverseKeys(ctsTree.root));

        } catch (Exception e) {
            loadingDisplay.doneLoading();
            e.printStackTrace();
        }
    }

    /*
     * Print any accounts that are on CS and not Moxie & viceversa
     */
    public void printDifferencesCSAndMoxie() {
        //create and start the loading spinner so we
        // know the program didn't crash while it loads the tree's
        Loading loadingDisplay = new Loading();
        Thread t = new Thread(loadingDisplay);
        t.start();



        try {
            File tmp = new File("DifferencesCSAndMoxiecsTree.btree");
            File tmp2 = new File("DifferencesCSAndMoxiemoxieTree.btree");

            RandomAccessFile file = new RandomAccessFile(tmp, "rw");
            RandomAccessFile file2 = new RandomAccessFile(tmp2, "rw");
            BTree csTree = new BTree(file);
            BTree moxieTree = new BTree(file2);

            List<String[]> cs =
                    nisEntries("nis://altair.cs.oswego.edu/cs.oswego.edu");
            for (String[] s : cs) {
                //UID = s[2]     GECOS = s[4]
                csTree.add("" + s[2], s[4]);
            }
            List<String[]> moxie = passwdEntries("/etc/passwd");
            for (String[] s : moxie) {
                //UID = s[2]     GECOS = s[4]
                moxieTree.add("" + s[2], s[4]);
            }

            //for each tree
            //remove whats in there from other tree
            for (String[] s : moxie) {
                csTree.remove("" + s[2]);
            }
            for (String[] s : cs) {
                moxieTree.remove("" + s[2]);
            }
            loadingDisplay.doneLoading();
            while (t.isAlive()) {
            }

            csTree.print(csTree.root, 0);
            System.out.println("Accounts on CS not on Moxie: \n" + csTree.traverseKeys(csTree.root));
            System.out.println("Accounts on Moxie not on CS: \n" + moxieTree.traverseKeys(moxieTree.root));

        } catch (Exception e) {
            loadingDisplay.doneLoading();
            e.printStackTrace();
        }
    }

    public void printAll() {
        //create and start the loading spinner so we
        // know the program didn't crash while it loads the tree's
        Loading loadingDisplay = new Loading();
        Thread t = new Thread(loadingDisplay);
        t.start();
        try {
            List<String[]> cs =
                    nisEntries("nis://altair.cs.oswego.edu/cs.oswego.edu");

            for (String[] s : cs) {
                System.out.println(s[2]);
            }
            loadingDisplay.doneLoading();
            while (t.isAlive()) {
            }
        } catch (Exception e) {
            loadingDisplay.doneLoading();
            e.printStackTrace();
        }
    }

    public ArrayList<String[]> nisEntries(String url) throws Exception {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.nis.NISCtxFactory");
        env.put(Context.PROVIDER_URL, url);
        Context ctx = new InitialContext(env);
        Context passwd = (Context) ctx.lookup("system/passwd");
        NamingEnumeration<Binding> it = passwd.listBindings("");

        // for demo just return all entries in ArrayList
        ArrayList<String[]> list = new ArrayList<String[]>();
        // for demo, just break out fields as array of strings
        while (it.hasMore()) {
            list.add(it.next().getObject().toString().split(":"));
        }
        return list;
    }

    public ArrayList<String[]> passwdEntries(String location) throws Exception {
        ArrayList<String[]> list = new ArrayList<String[]>();
        Scanner sc = new Scanner(new File(location));

        while (sc.hasNextLine()) {
            list.add(sc.nextLine().split(":"));
        }

        return list;
    }

    public BTree runWordTest() {

        //create and start the loading spinner so we
        // know the program didn't crash
        Loading loadingDisplay = new Loading();
        Thread t = new Thread(loadingDisplay);
        t.start();
        try {
            File tmp = new File("DifferencesCSAndMoxiecsTree.btree");
            RandomAccessFile file = new RandomAccessFile(tmp, "rw");
            BTree btree = new BTree(file);
            List<String> r = new LinkedList();

            File f = new File("words.txt");
            Scanner br;

            br = new Scanner(new FileReader(f));
            while (br.hasNextLine()) {
                r.add(br.nextLine());
            }
            for (String s : r) {
                if (s != null) {
                    btree.add(s, "a");
                }
            }

            for (String s : r) {
                if (s != null) {
                    btree.remove(s);
                }
            }

            loadingDisplay.doneLoading();
            while (t.isAlive()) {
            }

            System.out.println("size of words.txt file: " + r.size());
            System.out.println("Tree after done adding and removing: ");
            return btree;
        } catch (Exception e) {
            loadingDisplay.doneLoading();
            e.printStackTrace();
            return null;
        }
    }
}

class Loading implements Runnable {

    String loading;
    String up;
    String right;
    String flat;
    String left;
    boolean stillLoading;

    public Loading() {
        loading = "Loading in file ";
        up = "|";
        right = "/";
        flat = "-";
        left = "\\";
        stillLoading = true;
    }

    @Override
    public void run() {
        while (stillLoading) {
            try {
                Thread.sleep(150);
                System.out.print(loading + up + "\r");
                Thread.sleep(150);
                System.out.print(loading + right + "\r");
                Thread.sleep(150);
                System.out.print(loading + flat + "\r");
                Thread.sleep(150);
                System.out.print(loading + left + "\r");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println(loading + "...Success!");
    }

    public void doneLoading() {
        stillLoading = false;
    }
}

