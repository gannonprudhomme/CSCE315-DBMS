package dbms;

import org.junit.Test;
import types.IntType;
import types.Type;
import types.Varchar;

import java.util.*;

import static org.junit.Assert.*;

public class DbmsTests {
    Dbms db = new Dbms();
    //union  I think this test is set up properly... I honestly don't really know
    //difference
    //product
    //select
    //
    @Test // Basically just tries to create table, then attempts to retrieve it from Dbms.getTable
    public void createTable_twoCols_createsCorrectAttributes() {
        // Arrange
        String tableName = "table0";
        List<String> columnNames = new ArrayList<>(
                Arrays.asList("varcharCol", "intCol")
        );
        List<Type> columnTypes = new ArrayList<>(
                Arrays.asList(new Varchar(20), new IntType())
        );
        List<String> primaryKeys = new ArrayList<>(
                Arrays.asList("varcharCol")
        );

        Dbms sut = new Dbms();

        // Act
        sut.createTable(tableName, columnNames, columnTypes, primaryKeys);

        // Assert
        TableRootNode result= sut.getTable(tableName);
        // assertNotNull(result); // First make sure this key actually exists

        Attribute col1 = new Attribute("varcharCol", 0, new Varchar(20), "");
        Attribute col2 = new Attribute("intCol", 1, new IntType(), "");

        // Make sure the attributes are what we expect
        assertEquals(result.getAttribute(0), col1);
        assertEquals(result.getAttribute(1), col2);
    }

    @Test // Insert values into the table and check that the correct row was created
    public void insertFromValues_alignedAttributes_doesInsert() {
        String tableName = "table0";
        createTable(tableName, 0);

        Object[] data = new Object[] { "string", 2 };

        List<Object> dataList = new ArrayList<>(
                Arrays.asList(data)
        );

        db.insertFromValues(tableName, dataList);

        TableRootNode table = db.getTable(tableName);
        RowNode ret = table.getRowNodes().get(0);

        RowNode expected = new RowNode(data);
        assertEquals(ret, expected);
    }

    @Test
    public void insertFromRelation_alignedAttributes_doesInsert() {
        String tableName0 = "table0", tableName1 = "table1";
        createTable(tableName0, 0);
        createTable(tableName1, 1);

        Object[] data0 = new Object[] { "string", 2 };
        Object[] data1 = new Object[] { "stuff" };

        // Assumes insertFromValues works as well
        db.insertFromValues(tableName0, Arrays.asList(data0));
        db.insertFromValues(tableName1, Arrays.asList(data1));

        db.insertFromRelation(tableName0, tableName1);

        TableRootNode table0 = db.getTable(tableName0);

        assertEquals(2, table0.getRowNodes().size()); // Should have two entries

        RowNode actual = table0.getRowNodes().get(1);
        RowNode expected = new RowNode(new Object[] { "stuff", 0 });

        //assertEquals(expected, actual);
    }

    @Test
    public void show_test(){
        String tableName0 = "table0", tableName1 = "table1";
        createTable(tableName0, 0);
        createTable(tableName1, 1);

        Object[] data0 = new Object[] { "stuff", 2 };
        Object[] data1 = new Object[] { "stuff" };

        // Assumes insertFromValues works as well
        db.insertFromValues(tableName0, Arrays.asList(data0));
        db.insertFromValues(tableName1, Arrays.asList(data1));
        db.show(tableName0);
        db.show(tableName1);
    }

    @Test
    public void union_doesCombineTables() {
        String tableName0 = "table0", tableName1 = "table1", tableName2 = "table2";
        ArrayList<Attribute> attributes = new ArrayList<>();
        Attribute col1 = new Attribute("varcharCol", 0, new Varchar(20), "");
        Attribute col2 = new Attribute("intCol", 1, new IntType(), "");
        attributes.add(col1);
        attributes.add(col2);


        Object[] rowData0 = new Object[] { "string", 1};
        Object[] rowData1 = new Object[] { "new", 1};
        Object[] rowData2 = new Object[] { "test", 0};

        RowNode row0 = new RowNode(rowData0);
        RowNode row1 = new RowNode(rowData1);
        RowNode row2 = new RowNode(rowData2);

        TableRootNode table0 = new TableRootNode(tableName0, attributes);
        TableRootNode table1 = new TableRootNode(tableName1, attributes);
        TableRootNode table2 = new TableRootNode(tableName2, attributes);

        table0.addRow(row0);
        table0.addRow(row1);

        table1.addRow(row0);
        table1.addRow(row2);

        table2.addRow(row0);
        table2.addRow(row1);
        table2.addRow(row2);

        db.tables.put(tableName0, table0);
        db.tables.put(tableName1, table1);
        db.tables.put(tableName2, table2);

        String newTable = db.union(tableName1, tableName2);
        TableRootNode unionTable = (TableRootNode) db.tables.get(newTable);
        // Assumes insertFromValues works as well


        String newTableName = db.union(tableName0, tableName1);

        TableRootNode unionNewTable = db.getTable(newTableName);

        //System.out.println("union test");
        assertEquals(unionTable.getRowNodes().size(), 3); // Should have three entries (since duplicate should be removed.)

        List<RowNode> manualRowNodes = db.getTable(newTable).getRowNodes();
        List<RowNode> unionRowNodes = db.getTable(unionNewTable.relationName).getRowNodes();

        assertEquals(manualRowNodes, unionRowNodes);
        //System.out.println("unionTest end");
    }

    // Creates a table with a row of {"one" 1} and updates it to {"two", 2}
    @Test
    public void update_doesUpdateTable() {
        String tableName = "table";
        createTable(tableName, 0);

        List<Object> data = new ArrayList<>(Arrays.asList("one", 1));

        db.insertFromValues(tableName, data);

        List<String> columnsToSet = new ArrayList<>(Arrays.asList("varcharCol", "intCol"));
        List<Object> valuesToSet = new ArrayList<>(Arrays.asList("two", 2));
        Condition condition = createCondition(0);

        db.update(tableName, columnsToSet, valuesToSet, condition);

        RowNode actual = db.tables.get(tableName).getRowNodes().get(0);
        RowNode expected = new RowNode(valuesToSet.toArray());

        assertEquals(actual, expected);
    }

    @Test // Inputs two identical rowNodes and checks if the Set only contains 1 RowNode total
    public void rowNode_hashCode_setRemovesDuplicates() {
        Object[] table1 = new Object[] { "string", 1};
        Object[] table2 = new Object[] { "string", 1};

        RowNode node = new RowNode(table1);
        RowNode node2 = new RowNode(table2);

        Set<RowNode> set = new HashSet<>();
        set.add(node); set.add(node2);

        System.out.println(set.size());
        assertEquals(set.size(), 1);
    }

    private Condition createCondition(int which) {
        Condition root = new Condition();
        if(which == 0) { // varcharCol == "one" || intCol == 1
            root.op = Operator.OR;
            Condition left = new Condition();
            left.op = Operator.EQUALS;
            left.left = new Attribute("intCol");
            left.right = "5";
            Condition right = new Condition();
            right.op = Operator.EQUALS;
            right.left = new Attribute("varcharCol");
            right.right = "one";

            root.left = left;
            root.right = right;
        }

        return root;
    }

    private Condition expected(int which) {
        Condition root = new Condition();
        if(which == 1) { // kind == "cat" || kind == "dog"
            root.op = Operator.OR;
            Condition left = new Condition();
            left.op = Operator.EQUALS;
            left.left = new Attribute("kind");
            left.right = "cat";
            Condition right = new Condition();
            right.op = Operator.EQUALS;
            right.left = new Attribute("kind");
            right.right = "dog";

            root.left = left;
            root.right = right;
        } else if(which == 2) { // kind == "cat" || (kind == "dog" && age > 5))
            root.op = Operator.OR;
            Condition b = new Condition();
            b.op = Operator.AND;
            Condition c = new Condition();
            c.op = Operator.GREATER;
            c.left = new Attribute("age");
            c.right = 5;
            Condition d = new Condition();
            d.op = Operator.EQUALS;
            d.left = new Attribute("kind");
            d.right = "dog";
            b.left = d;
            b.right = c;
            Condition e = new Condition();
            e.op = Operator.EQUALS;
            e.left = new Attribute("kind");
            e.right = "cat";

            root.left = e;
            root.right = b;
        }

        return root;
    }

    // Creates a basic table
    // Which is just a selector
    private void createTable(String tableName, int which) {
        if(which == 0) {
            List<String> columnNames = new ArrayList<>(
                    Arrays.asList("varcharCol", "intCol")
            );
            List<Type> columnTypes = new ArrayList<>(
                    Arrays.asList(new Varchar(20), new IntType())
            );
            List<String> primaryKeys = new ArrayList<>(
                    Arrays.asList("varcharCol")
            );

            db.createTable(tableName, columnNames, columnTypes, primaryKeys);
        } else if(which == 1) {
            List<String> columnNames = new ArrayList<>(
                    Arrays.asList("varcharCol")
            );
            List<Type> columnTypes = new ArrayList<>(
                    Arrays.asList(new Varchar(20))
            );
            List<String> primaryKeys = new ArrayList<>(
                    Arrays.asList("varcharCol")
            );

            db.createTable(tableName, columnNames, columnTypes, primaryKeys);
        }
    }

    @Test
    public void product_test() {
        String tableName0 = "table0";
        String tableName1 = "table1";
        createTable(tableName0,  0);
        createTable(tableName1, 1);
        Object[] table0_data0 = new Object[] {"stuff", 1};
        Object[] table0_data1 = new Object[] {"stuff2", 2};
        Object[] table1_data0 = new Object[] {"stuff3", 3};
        Object[] table1_data1 = new Object[] {"stuff4", 4};
        RowNode table0_row0 = new RowNode(table0_data0);
        RowNode table0_row1 = new RowNode(table0_data1);
        RowNode table1_row0 = new RowNode(table1_data0);
        RowNode table1_row1 = new RowNode(table1_data1);
        db.tables.get(tableName0).addRow(table0_row0);
        db.tables.get(tableName0).addRow(table0_row1);
        db.tables.get(tableName1).addRow(table1_row0);
        db.tables.get(tableName1).addRow(table1_row1);

        String newTableName = db.product(tableName0, tableName1);
        TableRootNode newTable = db.getTable(newTableName);
        db.show(newTableName);
    }
    @Test
    public void difference_test() {
        String tableName0 = "table0";
        String tableName1 = "table1";
        createTable(tableName0,  0);
        createTable(tableName1, 0);
        Object[] data0 = new Object[] {"stuff", 1};
        Object[] data1 = new Object[] {"stuff3", 3};
        RowNode table0_row0 = new RowNode(data0);
        RowNode table1_row0 = new RowNode(data1);
        db.tables.get(tableName0).addRow(table0_row0);
        db.tables.get(tableName1).addRow(table1_row0);
        String newTableName = db.difference(tableName0, tableName1);
        TableRootNode newTable =  db.getTable(newTableName);
        db.show(newTableName);
    }
}
