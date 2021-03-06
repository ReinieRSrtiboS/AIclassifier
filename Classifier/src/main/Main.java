package main;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    private static final int smoother = 1;
    private static int CHI_SQUARE_SIZE = 300;
    private static int NUMBER_OF_CLASSES = 0;

    private static String[] classnames;

    private static int[] documents;

    private static Map<String, BigDecimal> vocabulary = new HashMap<>();  //de lijst met woorden die worden gebruikt

    private static Map<String, int[]> wordCount = new HashMap<>(); //telt per woord het aantal per class

    private static Map<String, int[]> chiSquare = new HashMap<>(); // telt per woord aantal texten dat hij inzit

    private static int[] words; //telt het aantal woorden per class

    private static int correct = 0;
    private static int incorrect = 0;


    public Main(int classes, int chi, String[] names) {
        NUMBER_OF_CLASSES = classes;
        CHI_SQUARE_SIZE = chi;
        documents = new int[NUMBER_OF_CLASSES + 1];
        words = new int[NUMBER_OF_CLASSES];
        classnames = names.clone();
    }

    public void learn(String path, int i) {
        train(normalize(readFile(path).split(" ")), i);

        for (String word : vocabulary.keySet()) {
            chiSquare(word);
        }

        for (String word : normalize(readFile(path).split(" "))) {
            chiSquare(word);
        }
    }

    public static void main(String[] args) {
        for (int i = 1; i < 11; i++) {
            train(i);
        }
        for (String word : wordCount.keySet()) {
            chiSquare(word);
        }
        double V = wordCount.keySet().size();

        double[] classChance = new double[NUMBER_OF_CLASSES];
        for (int i = 0 ; i  < NUMBER_OF_CLASSES; i++) {
            classChance[i] = (double) documents[i + 1] / (double) documents[0] * 100;
        }

        for (int i = 1; i < 11; i++) {
            //TODO fix path
            File dir = new File("C:\\Users\\Reinier\\Downloads\\corpus-mails\\corpus-mails\\corpus\\part" + i + "/");
            for (File file : dir.listFiles()) {
                double spamKans = 0;
                double hamKans = 0;
                String[] document = normalize(readFile(file.getPath()).split(" "));
                for (String word : document) {
                    if (vocabulary.containsKey(word)) {
                        spamKans += Math.log(chanceOfWord(word, V, 0));
                        hamKans += Math.log(chanceOfWord(word, V, 1));
                    }
                }
                spamKans += Math.log(classChance[0]);
                hamKans += Math.log(classChance[1]);
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

    private static String chanceOfWord(String[] document) {
        int[] chance = new int[NUMBER_OF_CLASSES];
        double V = wordCount.keySet().size();
        double[] classChance = new double[NUMBER_OF_CLASSES];

        for (int i = 0; i < NUMBER_OF_CLASSES; i++) {
            chance[i] = 0;
        }
        for (String word : document) {
            if (vocabulary.containsKey(word)) {
                for (int i = 0; i < NUMBER_OF_CLASSES; i++) {
                    chance[i] += Math.log(chanceOfWord(word, V, i));
                }
            }
        }
        for (int i = 0 ; i  < NUMBER_OF_CLASSES; i++) {
            classChance[i] = (double) documents[i + 1] / (double) documents[0] * 100;
            chance[i] += Math.log(classChance[i]);
        }

        int result = 0;
        for (int i = 0; i < NUMBER_OF_CLASSES; i++) {
            result = chance[i] > result ? i : result;
        }
        return classnames[result];
    }

    private static void train(int i) {
        //TODO maakt path compatible
        File dir = new File("C:\\Users\\Reinier\\Downloads\\corpus-mails\\corpus-mails\\corpus\\part" + i + "/");
        for (File file : dir.listFiles()) {
            if (file.getName().contains("spmsg")) {
                train(normalize(readFile(file.getPath()).split(" ")), 0);
            } else {
                train(normalize(readFile(file.getPath()).split(" ")), 1);
            }
        }
    }

    private static void chiSquare(String word) {
        if (!vocabulary.containsKey(word)) {
            BigDecimal chiSquare = calculate(word);
            if (vocabulary.size() > CHI_SQUARE_SIZE) {
                BigDecimal minimal = new BigDecimal(0);
                for (String key : vocabulary.keySet()) {
                    minimal = minimal.min(vocabulary.get(key));
                }
                if (chiSquare.compareTo(minimal) == 1) {
                    for (String key : vocabulary.keySet()) {
                        if (vocabulary.get(key) == minimal) {
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
            BigDecimal chiSquare = calculate(word);
            vocabulary.replace(word, chiSquare);
        }
    }

    private static BigDecimal calculate(String word) {
        int[][] table = new int[3][NUMBER_OF_CLASSES + 1];

        int total = 0;
        for (int i = 0; i < NUMBER_OF_CLASSES; i++) {
            if (chiSquare.containsKey(word)) {
                table[0][i] = chiSquare.get(word)[i];
                total += table[0][i];
            } else {
                table[0][i] = 0;
            }
        }
        table[0][NUMBER_OF_CLASSES] = total;

        total = 0;
        for (int i = 0; i < NUMBER_OF_CLASSES; i++) {
            table[1][i] = documents[i + 1] - table[0][i];
            total += table[1][i];
            table[2][i] = table[0][i] + table[1][i];
        }
        table[1][NUMBER_OF_CLASSES] = total;

        table[2][NUMBER_OF_CLASSES] = documents[0];

//        System.out.println(word + " " + Arrays.toString(table[0]) + " " + Arrays.toString(table[1]) + " " + Arrays.toString(table[2]));

        BigDecimal result = new BigDecimal(0);
        for (int i = 0; i < NUMBER_OF_CLASSES; i++) {
            for (int j = 0; j < 2; j++) {
//                BigDecimal expected = new BigDecimal(1);
//                expected = expected.multiply(new BigDecimal(table[2][i]));
//                expected = expected.multiply(new BigDecimal(documents[i + 1]));
//                expected = expected.divide(new BigDecimal(documents[0]), 50, RoundingMode.HALF_UP);
//                BigDecimal noemer = new BigDecimal(table[j][i]).subtract(expected);
//                noemer = noemer.multiply(noemer);
//                noemer = noemer.divide(expected, 50, RoundingMode.HALF_UP);
//                result = result.add(noemer);

                double expected = table[2][i] * documents[i + 1] / documents[0];
                double finish = Math.pow((table[j][i] - expected), 2) / expected;
                result = result.add(new BigDecimal(finish));
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
        Set<String> allWords = new HashSet<>();

        documents[0]++;
        documents[i + 1]++;

        for (String word : document) {
            if (!allWords.contains(word)) {
                allWords.add(word);
                int[] table;
                if (chiSquare.containsKey(word)) {
                    table = chiSquare.get(word);
                    table[i] = chiSquare.get(word)[i] + 1;
                } else {
                    table = new int[NUMBER_OF_CLASSES];
                    table[i] = 1;
                }
                chiSquare.put(word, table);
            }
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
        }
    }

    private static double chanceOfWord(String word, double V, int i) {
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
