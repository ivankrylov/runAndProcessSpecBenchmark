package com.azul;

//import com.sun.tools.internal.ws.wsdl.document.jaxws.Exception;
import java.lang.Exception;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.*;

public class ParseSpecResults {

    Options options;
    Map<String, String[]> args;

    public static void main(String[] args) throws java.lang.Exception {
        ParseSpecResults instance = new ParseSpecResults();
        if (instance.parseArguments(args)) return;

        String directory=instance.GetParameters("d");
        String suffix=instance.GetParameters("f");

        String[] experiments = instance.GetParametersMultivalue("s");
        String[] readynow = instance.GetParametersMultivalue("r");
        String testName = instance.GetParameters("t");

        String[] logs = instance.makeFilePaths(directory, "console", testName, experiments, suffix);
        String[] profiles_in = instance.makeFilePaths(directory, "profile_in", testName, experiments, suffix);
        String[] profiles_out = instance.makeFilePaths(directory, "profile_out", testName, experiments, suffix);
        String[] spec_reports = instance.makeFilePaths(directory, "spec_results", testName, experiments, null);
        String[] readynow_reports = instance.makeFilePaths(directory, "readynow", testName, readynow, suffix);

        if (experiments.length > 0 ) {
            System.out.println("\n\n");
            instance.print_one_test_table(testName, logs, experiments, spec_reports, profiles_in, profiles_out);
        }

        if (readynow.length > 0 ) {
            System.out.println("\n\n");
            instance.print_one_test_pp_table(testName, readynow, readynow_reports);
        }
    }

    void print_wiki_table_1_line(ArrayList<String> strs) {
        System.out.print("||");
        for (String s : strs) System.out.print(" " + s + " ||");
        System.out.println("");
    }


    void print_one_test_table(String testName, String[] logs, String[] experiments, String[] spec_reports, String[] profiles_ins, String[] profiles_out) throws java.lang.Exception {
        ArrayList<LinkedHashMap<String, Integer>> maps= new ArrayList<LinkedHashMap<String, Integer>>(logs.length);
        int i=0;


        for (String s : logs) maps.add(collectDeoptData(s));

        System.out.println("\nShort report");


        {
            ArrayList<String> strs = new ArrayList<>(5);
            strs.add(testName);
            for (i=0; i < 4; i++) strs.add(experiments[i]);
            print_wiki_table_1_line(strs);
        }
        {
            ArrayList<String> strs = new ArrayList<>(5);
            strs.add("Score (ops/m)");
            for (String s : logs) strs.add(getCompositeResult(s));
            print_wiki_table_1_line(strs);
        }
        {
            ArrayList<String> strs = new ArrayList<>(5);
            String key = "Total number of deoptimizations";
            strs.add(key);
            for (i=0; i < 4; i++) strs.add(maps.get(i).get(key).toString());
            print_wiki_table_1_line(strs);
        }



        System.out.println("\nLong report");


        {
            ArrayList<String> strs = new ArrayList<>(5);
            strs.add(testName);
            for (i=0; i < 4; i++) strs.add(experiments[i]);
            print_wiki_table_1_line(strs);
        }


        {
            ArrayList<String> strs = new ArrayList<>(5);
            strs.add("Score (ops/m)");
            for (String s : logs) strs.add(getCompositeResult(s));
            print_wiki_table_1_line(strs);
        }

        {
            ArrayList<String> strs = new ArrayList<>(5);
            strs.add("SpecJVM Reports:");
            for (String s : spec_reports) strs.add("  [[ http://release.azulsystems.com"+ s + " | SPECjvm report ]]  ");
            print_wiki_table_1_line(strs);
        }

        {
            ArrayList<String> strs = new ArrayList<>(5);
            strs.add("Output Logs:");
            for (String s : logs) strs.add(" [[ http://release.azulsystems.com"+ s + " | Console output ]]   ");
            print_wiki_table_1_line(strs);
        }

        {
            ArrayList<String> strs = new ArrayList<>(5);
            strs.add("profiles in:");
            for (String s : profiles_ins) strs.add(" [[ http://release.azulsystems.com"+ s + " | Profile In ]]   ");
            print_wiki_table_1_line(strs);
        }


        {
            ArrayList<String> strs = new ArrayList<>(5);
            strs.add("profiles out:");
            for (String s : profiles_out) strs.add(" [[ http://release.azulsystems.com"+ s + " | Profile Out ]]   ");
            print_wiki_table_1_line(strs);
        }

        for (Map.Entry<String, Integer> entry : maps.get(0).entrySet()) {
            ArrayList<String> strs = new ArrayList<>(5);
            String key = entry.getKey();
            strs.add(key);
            for (i=0; i < 4; i++) strs.add(maps.get(i).get(key).toString());
            print_wiki_table_1_line(strs);
        }


    }


    void print_one_test_pp_table(String benchmark, String[] experiments, String[] filenames) throws java.lang.Exception {
        ArrayList<LinkedHashMap<String, String>> maps= new ArrayList<LinkedHashMap<String, String>>(filenames.length);
        int i=0;

        for (String s : filenames) maps.add(collectPPData(s));

        {
            ArrayList<String> strs = new ArrayList<>(3);
            strs.add(benchmark);
            for (i=0; i < 2; i++) strs.add(experiments[i]);
            print_wiki_table_1_line(strs);
        }

        {
            ArrayList<String> strs = new ArrayList<>(5);
            strs.add("ReadyNow Reports:");
            for (String s : filenames) strs.add("  [[ http://release.azulsystems.com"+ s + " | ReadyNow report ]]  ");
            print_wiki_table_1_line(strs);
        }


        for (Map.Entry<String, String> entry : maps.get(0).entrySet()) {
            ArrayList<String> strs = new ArrayList<>(3);
            String key = entry.getKey();
            strs.add(key);
            for (i=0; i < 2; i++) strs.add(maps.get(i).get(key).toString());
            print_wiki_table_1_line(strs);
        }
    }


    String[] makeFilePaths(String dir, String name, String testName, String[] value, String suffix) {
        List<String> strs = new ArrayList<>(value.length);
        for (String s : value)  strs.add(makeFilePath(dir, name, testName, s, suffix));
        String[] result=new String[strs.size()];
        strs.toArray(result);
        return result;
    }

    String makeFilePath(String dir, String name, String testName, String value, String suffix) {
        if (suffix == null)
          return dir + "/" + name + "." + testName + "." + value;
        else
            return dir + "/" + name + "." + testName + "." + value + "." + suffix;
    }

    String GetParameters(String param) {
        return  args.get(param)[0];
    }

    String[] GetParametersMultivalue(String param) {
        String[] result=args.get(param);
        if (result == null) return new String[0];
        return result;
    }

    LinkedHashMap<String, String> collectPPData(String filename) throws java.lang.Exception   {
        String key="class loaders:";
        boolean startedCollecting=false;
        LinkedHashMap<String, String> result = new LinkedHashMap<>(30);

        List<String> lines = Files.readAllLines(Paths.get(filename));

        for ( String s : lines) {
            if (!startedCollecting) {
                if (s.startsWith(key)) startedCollecting=true;
            }

            if (startedCollecting) {
                String[] subs = s.split(":");
                if (subs.length == 2) {
                    result.put(subs[0].trim(), subs[1].trim() );
                }
                if (subs.length == 3) {
                    result.put(subs[0].trim(), subs[1].trim() + " / " + subs[2].trim());
                }
            }
        }
        return result;
    }

    LinkedHashMap<String, Integer> collectDeoptData(String filename) throws java.lang.Exception   {
        String key="Total number of ";
        boolean startedCollecting=false;
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>(30);

        List<String> lines = Files.readAllLines(Paths.get(filename));

        for ( String s : lines) {

            if (!startedCollecting) {
                if (s.startsWith(key)) startedCollecting=true;
            }

            if (startedCollecting) {
                String[] subs = s.split(":");
                result.put(subs[0].trim(), new Integer(subs[1].split(" ")[1]));
            }
        }
        return result;
    }

    String getCompositeResult(String filename) throws java.lang.Exception   {
        String key="Noncompliant composite result: ";
        String result;
        try (Stream<String> lines = Files.lines(Paths.get(filename))) {
            result= lines
            .filter(s -> s.startsWith(key))
            .map( s -> s.split(" ")[3] )
            .collect(Collectors.joining(", "));
        }
        return result;
    }

    // Return true if should exit
    boolean parseArguments(String[] commandLineArguments) throws java.lang.Exception  {

        Options options = new Options();

        options.addOption("h", "help", false ,  "This helpscreen");

        Option descriptionsOption = new Option("s", "source-descriptions", true, "list of descriptions for the source space separated");
        //"s", "source", "list of specjvm log files with paths");
        descriptionsOption.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(descriptionsOption);

        Option r = new Option("r", "readynow-descriptions", true, "list of descriptions for readynow  reports");
        //"s", "source", "list of specjvm log files with paths");
        r.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(r);


        Option d= new Option("d", "directory", true , "The directory");
        d.setArgs(1);
        options.addOption(d);

        Option f= new Option("f", "filesuffix", true , "Suffix for each file");
        f.setArgs(1);
        options.addOption(f);

        Option t= new Option("t", "title", true , "The title of the run");
        t.setArgs(1);
        options.addOption(t);


        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, commandLineArguments);

        if ( ( commandLine.getOptions().length == 0 ) ||  (commandLine.getOptionValue("h") != null) )  {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "Main", options );
            return true;
        }

        args  = new HashMap<String, String[] >();

        for (String s : Arrays.asList("s", "r")) {
            String[] subs=commandLine.getOptionValues(s);
            if (subs == null) continue;
            args.put(s, commandLine.getOptionValues(s));
        }

        for (String s : Arrays.asList("t", "f", "d", "j")) {
            args.put(s, new String[] { commandLine.getOptionValue(s) } );
        }

        return false;
    }
}

