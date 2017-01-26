package main;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class main {

    private static final int smoother = 1;
    private static final int MAX_SIZE = 300;
    private static final int NUMBER_OF_CLASSES = 2;

    private static int[] documents = new int[NUMBER_OF_CLASSES + 1];

    private static Map<String, Integer> vocabulary = new HashMap<>();  //de lijst met woorden die worden gebruikt

    private static Map<String, int[]> wordCount = new HashMap<>(); //telt per woord het aantal per class

    private static Map<String, int[]> wordTable = new HashMap<>();  //de chi-square tabel

    private static int[] words = new int[NUMBER_OF_CLASSES]; //telt het aantal woorden per class

    private static int correct = 0;
    private static int incorrect = 0;


    public static void main(String[] args) {
        for (int i = 1; i < 11; i++) {
            train(i);
        }
//        train(1);
        double V = wordCount.keySet().size();

        double[] classChance = new double[NUMBER_OF_CLASSES];
        for (int i = 0 ; i  < NUMBER_OF_CLASSES; i++) {
            classChance[i] = (double) documents[i + 1] / (double) documents[0] * 100;
        }

        for (int i = 1; i < 11; i++) {
            //TODO fix path
            File dir = new File("C:\\Users\\reinier\\Downloads\\corpus-mails\\corpus-mails\\corpus\\part" + i + "/");
            for (File file : dir.listFiles()) {
                double spamKans = 0;
                double hamKans = 0;
                String[] document = normalize(readFile(file.getPath()).split(" "));
                for (String word : document) {
                    if (vocabulary.containsKey(word)) {
                        spamKans += Math.log(classify(word, V, 0));
                        hamKans += Math.log(classify(word, V, 1));
                    }
                }
                hamKans += Math.log(classChance[1]);
                spamKans += Math.log(classChance[0]);
//                System.out.println("ham " + hamKans + " spam " + spamKans);
                if ((hamKans > spamKans && file.getName().contains("-")) || (spamKans > hamKans && file.getName().contains("spmsg"))) {
                    correct++;
                } else {
                    incorrect++;
                }
            }
        }
        System.out.println("correct " + correct);
        System.out.println("incorrect " + incorrect);
        double percentage = (correct / (double) (correct + incorrect)) * 100;
        System.out.println(percentage + "%");
    }

    private static void train(int i) {
        //TODO maakt path compatible
        File dir = new File("C:\\Users\\reinier\\Downloads\\corpus-mails\\corpus-mails\\corpus\\part" + i + "/");
        for (File file : dir.listFiles()) {
            documents[0]++;
            if (file.getName().contains("spmsg")) {
                train(normalize(readFile(file.getPath()).split(" ")), 0);
                documents[1]++;
            } else {
                train(normalize(readFile(file.getPath()).split(" ")), 1);
                documents[2]++;
            }
        }
    }

    private static void chiSquare(String word) {
        if (!vocabulary.containsKey(word)) {
            int chiSquare = calculate(word);
            if (vocabulary.size() > MAX_SIZE) {
                int min = Collections.min(vocabulary.values());
                if (chiSquare > min) {
                    for (String key : vocabulary.keySet()) {
                        if (vocabulary.get(key) == min) {
                            vocabulary.remove(key);
                            break;
                        }
                    }
                    vocabulary.put(word, chiSquare);
                }
            } else {
                vocabulary.put(word, chiSquare);
            }
        } else {
            int chiSquare = calculate(word);
            vocabulary.replace(word, chiSquare);
        }
    }

    private static int calculate(String word) {
        double[][] table = new double[NUMBER_OF_CLASSES + 1][3];

        int total = 0;
        for (int i = 0; i < NUMBER_OF_CLASSES; i++) {
            if (wordTable.containsKey(word)) {
                total += wordTable.get(word)[i];
                table[i][0] = wordTable.get(word)[i];
            } else {
                table[i][0] = 0;
            }
        }
        table[NUMBER_OF_CLASSES][0] = total;
        table[NUMBER_OF_CLASSES][2] = total;
        for (int i = 0; i < NUMBER_OF_CLASSES; i++) {
            table[i][1] = words[i] - table[i][0];
            table[i][2] = table[i][0] + table[i][1];
            table[NUMBER_OF_CLASSES][2] += table[i][0] + table[i][1];
        }


        int result = 0;
        for (int i = 0; i < NUMBER_OF_CLASSES; i++) {
            for (int j = 0; j < 2; j++) {
                double expected = table[i][2] * table[NUMBER_OF_CLASSES][j] / (table[NUMBER_OF_CLASSES][2]);
                result += Math.pow((table[i][j] - expected), 2) / expected;
            }
        }
        return result;
    }


    //moet beter worden
    //haalt hoofdletters weg en shit
    private static String[] normalize(String[] document) {
        String[] result = new String[document.length];
        for (int i = 0; i < document.length; i++) {
            result[i] = document[i].toLowerCase();
        }
        return result;
    }

    private static void train(String[] document, int i) {
        for (String word : document) {
            words[i] = words[i] + 1;
            int[] table;
            if (wordCount.containsKey(word)) {
                table = wordCount.get(word);
                table[i] = wordCount.get(word)[i] + 1;
            } else {
                table = new int[NUMBER_OF_CLASSES];
                table[i] = 1;
            }
            wordCount.put(word, table);
            chiSquare(word);
        }
    }

    private static double classify(String word, double V, int i) {
        double occurrence = wordCount.containsKey(word) ? wordCount.get(word)[i] : 0;
        double teller = words[i] + (smoother * V);
        double noemer = occurrence + smoother;
        double result = noemer / teller;
        return result;
    }

    //geeft de text van een bestand
    private static String readFile(String path) {
        byte[] encoded = new byte[0];
        try {
            encoded = Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            return new String(encoded, "UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "ERROR";
    }

}
