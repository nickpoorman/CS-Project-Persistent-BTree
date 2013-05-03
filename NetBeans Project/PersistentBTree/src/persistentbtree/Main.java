package persistentbtree;

public class Main {

    public static void main(String[] args) {
        if (args.length > 1) {
            System.out.println("Please enter only one argument.");
        }
        if (args.length < 1) {
            System.out.println("Please enter an argument to run a test: \n" +
                    " AccountsTest [<number of test>]\n" +
                    "a - Prints out local accounts on Moxie that are neither in CS nor CTS. " +
                    "\n" +
                    "b - Prints out mismatches in uid or gecos fields of local vs those on CS or CTS" +
                    "\n" +
                    "c - Print any accounts that are on CS and not CTS & viceversa" +
                    "\n" +
                    "d - Print any accounts that are on CS and not Moxie & viceversa" +
                    "\n" +
                    "e - Run all tests on BTree with words file read in and delete" +
                    "\n" +
                    "f - Run and print out all tests (including word file test)" +
                    "\n" +
                    "example: \n" +
                    "    java -jar AccountsTest.jar b \n" +
                    "example: \n" +
                    "    ./Accounts.sh b" +
                    "\n");
        } else {
            char c = args[0].charAt(0);

            if (c == 'a') {
                Tester t = new Tester();
                t.printMoxieAccountsNotCsNorCTS();
            } else if (c == 'b') {
                Tester t = new Tester();
                t.printMismatchedUIDandGECOSonCTAandMoxie();
            } else if (c == 'c') {
                Tester t = new Tester();
                t.printDifferencesCSAndCTS();
            }else if (c == 'd') {
                    Tester t = new Tester();
                    t.printDifferencesCSAndMoxie();
                } else if (c == 'e') {
                    Tester t = new Tester();
                    BTree tree = t.runWordTest();
                    tree.print(tree.root, 0);
                } else if (c == 'f') {
                    Tester t = new Tester();
                    t.printMoxieAccountsNotCsNorCTS();
                    t.printMismatchedUIDandGECOSonCTAandMoxie();
                    t.printDifferencesCSAndCTS();
                    t.printDifferencesCSAndMoxie();
                    t.runWordTest();
                } else if (c == 'l') {
                    Tester t = new Tester();
                    t.loadAccounts();
                //tree.print(tree.root, 0);

                } else {
                    System.out.println("You did not enter a valid argument.");
                }
//        Tester t = new Tester();
//        t.printDifferencesCSAndMoxie();
            }
        }
}
    




