package dbms;

import types.Type;
import types.Varchar;

import java.util.*;

/**
 * The internal representation of our database
 * Contains all of the tables, and maybe their rows as well?
 */
public class Dbms implements IDbms {
    // Maps each table name to their internal representation
    // Includes temporary tables as well
    public HashMap<String, TableRootNode> tables;
    //private HashMap<String, Object> tempTables;

    // Should we have a temporary/local tables?

    public Dbms() {
        tables = new HashMap();
    }
    //public Dbms() { tempTables = new HashMap(); }

    @Override
    public void createTable(String tableName, List<String> columnNames, List<Type> columnTypes, List<String> primaryKeys) {
        if((columnNames.size() != columnTypes.size())) { // Doesn't need to be an equal amount of primaryKeys & columnTypes
            System.out.println("Improper input");
            return;
        }
        ArrayList<Attribute> attributesList = new ArrayList<Attribute>();
        Iterator<String> iter = primaryKeys.iterator();
        Iterator<Type> iterType = columnTypes.iterator();
        int i = 0; // Rename to columnIndex? Why not just do a for i = 0 loop?
        for(String element : columnNames){ //iterate through, make the attribute list
            String pkeyel = ""; // How is this even used?
            // String pkeyel = iter.next();
            Type typeel = iterType.next();
            Attribute temp;
            temp = new Attribute(element, i, typeel, pkeyel);
            i++;
            attributesList.add(temp); ///this creates the attributes list
        }
        TableRootNode table = new TableRootNode(tableName, attributesList); //creates table
        tables.put(tableName, table); //puts new table root node into hashmap with name as key
    }

    @Override
    public void insertFromRelation(String tableInsertInto, String tableInsertFrom) {
        //we will need to work on handling the creation of temporary tables for insert command


        //Works by taking all the leaves of the tableInsertFrom and adding them to tableInsertInto
        //essentially just take the arraylist of row nodes in tablefrom and append it to the array list of rownodes in insert into
        TableRootNode tableFrom = tables.get(tableInsertFrom);
        ArrayList<Attribute> attListFrom = tableFrom.getAttributes();
        TableRootNode tableInto = tables.get(tableInsertInto);
        ArrayList<Attribute> attListInto = tableInto.getAttributes();
        /* if(attListFrom != attListInto){ //may not work properly as a comparison, if so just remove since data should be clean
            System.out.println("Mismatched attirbutes");
            return;
        } */



        // when an attribute in the attributes from list matches the name and type of an attribute in the attributes into list,
        //this loop will create a tuple of the form [indexFrom, indexInto] to inform the next code block which columns in the
        //FROM table to map to the which columns in the INTO table.
        ArrayList<int[]> tuples = new ArrayList<>();
        for(Attribute attributeInto : attListInto) {
            boolean found = false;
            for(Attribute attributeFrom : attListFrom) {
                if((attributeFrom.getType() == attributeInto.getType()) && (attributeFrom.getName() == attributeInto.getName())){
                    int[] newTup = new int[]{attributeFrom.index, attributeInto.index};
                    attListFrom.remove(attributeFrom);
                    tuples.add(newTup);
                    found = true;
                    break;
                }
            }
            if(!found){
                int[] newTup = new int[]{-1, attributeInto.index};
                tuples.add(newTup);
            }
        }
        /*while(!(attListFrom.isEmpty())){
            for(Attribute attributeFrom : attListFrom){
                Object[] newTup = new Object[]{attributeFrom.index, null};
                tuples.add(newTup);
            }
        }*/



        List<RowNode> rowListFrom = tableFrom.getRowNodes();
        for(RowNode rowFrom : rowListFrom){ //pulls each row node from table from
            Object[] newDataFields = new Object[attListInto.size()];
            for(int[] tuple : tuples){
                int fromIndex = tuple[0];
                int toIndex = tuple[1];
                if(fromIndex == -1){ //if the value is not present in the from table
                    newDataFields[toIndex] = null; //set value to null
                }else{
                    newDataFields[toIndex] = rowFrom.getDataField(fromIndex);
                }
            }
            RowNode newRow = new RowNode(newDataFields);
            tableInto.addRow(newRow);  //inserts them into table into
        }
    }

    @Override // Should be done
    public void insertFromValues(String tableInsertInto, List<Object> valuesFrom) {
        //verify that the attributes match up, and then add a new node to rownodes
        //this verification is currently fairly naive, as it simply checks the length of the list versus
        //the size of the attribute list of the table it's being inserted into.

        TableRootNode temp = (TableRootNode) tables.get(tableInsertInto);
        int lengAttributes = temp.getAttributeSize();
        if(valuesFrom.size() != lengAttributes){
            System.out.println("Mismatched attribute length");
            return;
        }
        Object[] rowVals = new Object[lengAttributes];
        int i = 0;
        for(Object val : valuesFrom){
            rowVals[i] = val;
            i++;
        }
        RowNode newRowNode = new RowNode(rowVals);//creates new row node
        temp.addRow(newRowNode);//adds row node

    }

    @Override
    public void update(String tableName, List<String> columnsToSet, List<Object> valuesToSetTo, Condition condition) {
        TableRootNode table = tables.get(tableName);

        //e.g. update animals set age == 10 if age >= 10.
        List<Integer> rowsToUpdate = new ArrayList<>(); // Contains the rows to update, by which rowIndex they are in

        // Iterate through all of table.rows
        List<RowNode> tableRows = table.getRowNodes();
        for(int i = 0; i < tableRows.size(); i++) {
            // Evaluate the condition and add it to rowsToUpdate if it's true
            if(Condition.evaluate(condition, tableRows.get(i), table)){ // Might need to pass in table
                rowsToUpdate.add(i); // Add its according index
            }
        }

        // Mapped as (column index : value to update column with)
        Map<Integer, Object> colsToUpdate = new HashMap<>();

        for(int i = 0; i < columnsToSet.size(); i++) {
            String colName = columnsToSet.get(i);
            Object valueToSet = valuesToSetTo.get(i);

            int colIndex = table.getAttributeWithName(colName).index;
            colsToUpdate.put(colIndex, valueToSet);
        }

        // Update the according rows
        for(int rowIndex : rowsToUpdate) {
            RowNode row = tableRows.get(rowIndex); // Pass by reference?

            // Iterate through colIndicesToUpdate and set the according value from valuesToSetTo
            for(Map.Entry<Integer, Object> colValuePair : colsToUpdate.entrySet()) {
                row.setDataField(colValuePair.getKey(), colValuePair.getValue());
            }

            // Not sure if RowNode is passed by reference or value, so this may not be necessary
            tableRows.set(rowIndex, row);
        }
    }

    @Override
    public String projection(String tableFrom, List<String> columnNames) {
        String tempTable = getTempTableName();
        ArrayList<Attribute> origAttributes = tables.get(tableFrom).getAttributes();
        ArrayList<Integer> indices = new ArrayList<>();
        ArrayList<Attribute> newAttributes = new ArrayList<>();

        int j = 0;
        for(Attribute att : origAttributes){ // find the indices of the columns we need to maintain
            if(columnNames.contains(att.getName())){
                indices.add(att.index);
                Attribute newAttribute;
                newAttribute = new Attribute(att.getName(), j, att.getType(), "blah");
                newAttributes.add(newAttribute);
                j++;
            }
        }

        TableRootNode newTable = new TableRootNode(tempTable, newAttributes);

        for(RowNode row : tables.get(tableFrom).getRowNodes()){ //iterate through tableFrom's rows
            Object[] data = new Object[j]; //create new dataFields object[]

            int i = 0;
            for(Integer index : indices){//iterate through column indices we're interested in
                data[i] = (row.dataFields[index]);//add data to dataFields
                i++;
            }
            RowNode newRow = new RowNode(data);
            newTable.addRow(newRow);
        }
        tables.put(tempTable, newTable);
        return tempTable;
    }

    @Override
    public String rename(String tableName, List<String> newColumnNames) { //should this really return a string?
        String newName = getTempTableName();
        ArrayList<Attribute> attributes = tables.get(tableName).getAttributes();
        List<RowNode> kids = tables.get(tableName).getRowNodes();
        TableRootNode tempTable = new TableRootNode(newName, attributes, kids);
        int i = 0;
        for(String name : newColumnNames){
            tempTable.setAttributeName(name, i);
            i++;
        }
        tables.put(newName, tempTable);
        return newName;
    }

    @Override
    public String union(String table1, String table2) {
        String newTable = getTempTableName(); //the output table name will be a combination of the two table names
        ArrayList<Attribute> newAttributes = tables.get(table1).getAttributes(); //*****requires matching Attributes*****
        List<RowNode> newRows = tables.get(table1).getRowNodes();
        List<RowNode> newRows2 = tables.get(table2).getRowNodes();
        newRows.addAll(newRows2);
        Set<RowNode> noDupes = new HashSet<>(newRows); //remove duplicates
        newRows.clear(); //clear list
        newRows.addAll(noDupes);  //add new children without duplicates

        TableRootNode newTableRoot = new TableRootNode(newTable, newAttributes, newRows);
        tables.put(newTable, newTableRoot); //add the union to the tables hashmap
        return newTable;
    }

    @Override
    public String select(String tableFrom, Condition condition){
        String tempTableName = getTempTableName();

        TableRootNode table = tables.get(tableFrom);
        ArrayList<Attribute> attributes = table.getAttributes();
        TableRootNode newTable = new TableRootNode(tempTableName, attributes);
        for(RowNode row : tables.get(tableFrom).getRowNodes()){ //iterate through row nodes
            boolean include = Condition.evaluate(condition, row, table);

            if(include == true){
                newTable.addRow(row);
            }
        }
        tables.put(tempTableName, newTable);
        return tempTableName;
    }

    @Override
    public String difference(String table1, String table2) {
        //String tempTable = getTempTableName();
        String tempTableName = getTempTableName();
        ArrayList<Attribute> tempAttributes = tables.get(table1).getAttributes();
        TableRootNode tempTable = new TableRootNode(tempTableName, tempAttributes);
        for(RowNode row : tables.get(table1).children){ //for all row nodes in table 1
            if(!(tables.get(table2).children.contains(row))){//if the row node is not in table 2
                tempTable.addRow(row); //place it in the new temp table (create a table with all elements in table 1 but not in table 2)
            }
        }
        tables.put(tempTableName, tempTable);//add new table to hash map

        return tempTableName;
    }

    @Override
    public String product(String table1, String table2) {
        String tempName = getTempTableName();
        ArrayList<Attribute> tempAttributes;
        tempAttributes = tables.get(table1).getAttributes();
        tempAttributes.addAll(tables.get(table2).getAttributes()); //creates attribute list with both sets of attributes
        TableRootNode tempTable = new TableRootNode(tempName, tempAttributes); //new table
        List<RowNode> tableOneLeaves = tables.get(table1).getRowNodes();//leaves from table 1
        List<RowNode> tableTwoLeaves = tables.get(table2).getRowNodes(); //leaves from table 2

        for(RowNode rowOne : tableOneLeaves){
            for(RowNode rowTwo : tableTwoLeaves){


                int lengOne = rowOne.getDataFields().length;
                int lengTwo = rowTwo.getDataFields().length;
                int leng = lengOne + lengTwo;
                Object[] newDataFields = new Object[leng];

                for(int i = 0; i < lengOne; i++){
                    newDataFields[i] = rowOne.getDataField(i);
                }
                for(int j = 0; j < lengTwo; j++){
                    newDataFields[j+lengOne] = rowTwo.getDataField(j);
                }
                RowNode newRow = new RowNode(newDataFields);
                tempTable.addRow(newRow);


            }
        }
        tables.put(tempName, tempTable);
        return tempName;
    }

    @Override
    public void show(String tableName) {
        String s;
        ArrayList<Attribute> attributes = tables.get(tableName).getAttributes();
        final int colWidth = 25;
        String line = "";
        s = " " + tableName + "\n" ;
        for(int k = 0; k< attributes.size(); k++) {
            s += "--------------------------";
        }
        if (attributes.size() == 0){
            s += "";
        } else {
            s += "|\n";
        }
        for(int i = 0; i< attributes.size(); i++) {
            Attribute attr = attributes.get(i);

            line += attributes.get(i).getName() ;

            if(attr.type instanceof Varchar) {
                line += ": Varchar(" + ((Varchar) attr.type).length + ")";
            } else {
                line += ": Int";
            }

            while(line.length()<(colWidth)) {
                line += " ";
            }

            if (i == (attributes.size()-1)){
                line += " |";
            } else {
                line += "|";
            }
            s += line;
            line = "";
        }
        s += "\n";
        for(int l = 0; l< attributes.size(); l++) {
            s += "--------------------------";
        }
        if (attributes.size() == 0){
            s += "";
        } else {
            s += "|\n";
        }

        TableRootNode table = (TableRootNode) tables.get(tableName);
        List<RowNode> rowList = table.getRowNodes();
        for(int i = 0; i< rowList.size(); i++) {
            RowNode currRow = rowList.get(i);
            for(int j = 0; j<currRow.getDataFields().length; j++) {
                line += currRow.getDataField(j) ;
                while(line.length()<colWidth) {
                    line += " ";
                }
                if (j == (currRow.getDataFields().length-1)){
                    line += " |";
                } else {
                    line += "|";
                }
                s += line;
                line = "";
            }
            s += "\n";
            for(int m = 0; m< attributes.size(); m++) {
                s += "--------------------------";
            }
            if (attributes.size() == 0){
                s += "";
            } else {
                s += "|\n";
            }
        }

        System.out.println(s);



    }

    @Override
    public void delete(String tableName, Condition condition) {
        TableRootNode table = tables.get(tableName);

        List<Integer> rowsToRemove = new ArrayList<>();

        List<RowNode> tableRows = table.getRowNodes();

        for(int i = 0; i < tableRows.size(); i++) {
            if(Condition.evaluate(condition, tableRows.get(i), table)) {
                rowsToRemove.add(i);
            }
        }

        // Remove from the back so that the order doesn't get messed up
        for(int i = rowsToRemove.size() - 1; i >= 0; i--) {
            RowNode removed = tableRows.remove((int) rowsToRemove.get(i));
        }
    }

    // Opens a table(table + .db) from storage
    @Override public void open(String table) {
        String fileName = table = ".db";

        // Call XMLStorageManager and load it in
    }

    // Save all changes to the relation(table) and close it(remove it from the table map?)
    @Override public void close(String table) {

    }

    // Write the table to the disk? Might want to change this to accept a default filepath
    @Override public void write(String table) {
        String fileName = table + ".db"; // Assuming table is in the tables map

        // Call XMLStorageManager and save it
    }

    // Exit from the interpreter, dunno what should happen here
    @Override
    public void exit() {
        //end the entire program, and save data
        //just call write and then kill the listener
    }

    // Removes the (key, value) pair with oldName and replaces it with newName
    public void renameTable(String oldName, String newName) {
        TableRootNode table = tables.remove(oldName);
        table.relationName = newName;
        tables.put(newName, table);
    }

    @Override
    public TableRootNode getTable(String tableName) {
        return tables.get(tableName);
    }

    private int tempCount = 0; // current temp table we're on
    private String getTempTableName() { return ("temp" + tempCount++); }
}
