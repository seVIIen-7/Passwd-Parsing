import java.io.*;
import java.util.*;

public class PasswdParsing {

    /**
     * main method args[0] is pwd.txt args[1] is group.txt
     * @param args args file
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        //if args is insufficient
        if (args.length < 2) {
            throw new RuntimeException("Please input enough files");
        }
        File pwd, group;
        //try if file path is invalid
        try {
            pwd = new File(args[0]);
            group = new File(args[1]);
        } catch (Exception e) {
            throw new RuntimeException("Please input valid file path");
        }

        try {
            new PasswdParsing().parser(pwd, group);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Internal error!");
        }

    }

    /**
     * main function to parse file and write into local file
     * @param pwd pwd file
     * @param group group file
     * @throws IOException
     */
    public void parser(File pwd, File group) throws IOException {
        // pwd data in lines
        List<String> pwdList = fileReader(pwd);
        // group data in lines
        List<String> groupList = fileReader(group);
        //group pwd data using hashmap, key is the name value is the info object
        Map<String, Info> infos =pwdParser(pwdList);
        //insert group data into map
        groupPaser(groupList, infos);
        //build json string
        String json = builder(infos);
        //write into file
        printer(json);
    }

    /**
     * group data into map
     * @param pwdList list of lines in pwd file
     * @return grouped data
     */
    private Map<String, Info> pwdParser(List<String> pwdList) {
        Map<String, Info> ret = new HashMap<String, Info>();
        //iterate lines in pwd list
        for (String pwd : pwdList) {
            //example: root:x:0:0:root:/root:/bin/bash
            //sperate by ":" so we split data
            String[] pwdArr = pwd.split(":");
            //we cam get username at 0, pid at 2, userfullname at 4
            //make an empty arr and extend it on demand
            ret.put(pwdArr[0], new Info(pwdArr[0], pwdArr[2],pwdArr[4], new String[0]));
        }
        return ret;
    }

    /**
     * insert group information into map
     * @param groupList
     * @param infos
     */
    private void groupPaser(List<String> groupList, Map<String, Info> infos) {
        //iterate lines in group list
        for (String group : groupList) {
            //example daemon:x:2:bin,daemon
            String[] groupArr = group.split(":");
            //roll name at 0, member at 4
            String role =  groupArr[0];
            //if no member in this roll, we skip this line
            if (groupArr.length < 4) {
                continue;
            }
            //find all members
            String[] users = groupArr[3].split(",");
            for (String user : users) {
                //see if we found this member in our map
                if (infos.containsKey(user)) {
                    Info info = infos.get(user);
                    //expend array by 1
                    info.group = Arrays.copyOf(info.group, info.group.length + 1);
                    //add this role
                    info.group[info.group.length - 1] = role;
                }
            }
        }
    }

    /**
     * build json String
     * @param infos map info
     * @return json string in pretty format
     */
    private String builder(Map<String, Info> infos) {
        StringBuilder stringBuilder = new StringBuilder();
        //first {
        stringBuilder.append("{\n");
        int i = 0;
        //make sure no , after last object
        Info last = null;
        for (Info info : infos.values()) {
            if (++i == infos.size()) {
                last = info;
                break;
            }
            stringBuilder.append(info.toString() + ",\n");
        }
        //add the last object
        stringBuilder.append(last + "\n");
        //close }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    /**
     * print json string to file
     * @param json json string
     * @throws FileNotFoundException
     */
    private void printer(String json) throws FileNotFoundException {
        //since we do cron, we want filename contains timestamp
        PrintStream ps = new PrintStream("output-" + System.currentTimeMillis() + ".txt");
        System.setOut(ps);
        System.out.println(json);
    }

    /**
     * read file and load data into memory
     * may optimize when data is large and avoid overflow
     * @param file file
     * @return list of data
     * @throws IOException
     */
    private  List<String> fileReader(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        List<String> ret = new ArrayList<String>();
        String tempString = null;
        while ((tempString = reader.readLine()) != null) {
           ret.add(tempString);
        }
        reader.close();
        return ret;
    }
}

/**
 * Information objecvt
 */
class Info{
    String userName;
    String uid;
    String full_name;
    String[] group;

    public Info(String userName, String uid, String full_name, String[] group) {
        this.userName = userName;
        this.uid = uid;
        this.full_name = full_name;
        this.group = group;
    }

    /**
     * rewrite toString method so it can print pretty json format
     * @return
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\t\t\"" + userName + "\":{\n" +
                "\t\t\t\t\"uid\": " + "\"" + uid + "\",\n" +
                "\t\t\t\t\"full_name\": " + "\"" + full_name + "\",\n" +
                "\t\t\t\t\"groups\": [");
        if (group.length == 0) {
            sb.append("]\n");
        } else {
            sb.append("\n");
            int i = 0;
            String last = null;
            for (String role : group) {
                if (++i == group.length) {
                    last = role;
                    break;
                }
                sb.append("\t\t\t\t\"" + role + "\",\n");
            }
            sb.append("\t\t\t\t\"" + last + "\"\n");
        }
        sb.append("\t\t}");
        return sb.toString();
    }
}
