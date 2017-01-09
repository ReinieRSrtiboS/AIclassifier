package main;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class main {

    private static final int smoother = 1;
    //chi-square test vervanger
    private static final int top = 1000;

    private static int documents = 0;
    private static int spamDocuments = 0;
    private static int hamDocuments = 0;

    private static Map<String, Integer> spam = new HashMap<>();
    private static Map<String, Integer> ham = new HashMap<>();

    private static int spamWords = 0;
    private static int hamWords = 0;

    private static int correct = 0;
    private static int incorrect = 0;


    public static void main(String[] args) {
        for (int i = 1; i < 11; i++) {
            train(i);
        }
//        train(1);
        double V = ham.keySet().size();
        for (String word : spam.keySet()) {
            V = ham.containsKey(word) ? V : V + 1;
        }
        double hamRatio = (double) hamDocuments /  (double) documents * 100;
        double spamRatio = (double) spamDocuments / (double) documents * 100;

        for (int i = 1; i < 11; i++) {
            //TODO fix path
            File dir = new File("C:\\Users\\reinier\\Downloads\\corpus-mails\\corpus-mails\\corpus\\part" + i + "/");
            for (File file : dir.listFiles()) {
                double spamKans = 0;
                double hamKans = 0;
                String[] document = normalize(readFile(file.getPath()).split(" "));
                for (String word : document) {
                    spamKans += Math.log(spam(word, V));
                    hamKans += Math.log(ham(word, V));
                }
                hamKans += Math.log(hamRatio);
                spamKans += Math.log(spamRatio);
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
            documents++;
            if (file.getName().contains("spmsg")) {
                trainSpam(normalize(readFile(file.getPath()).split(" ")));
                spamDocuments++;
            } else {
                trainHam(normalize(readFile(file.getPath()).split(" ")));
                hamDocuments++;
            }
        }
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

    private static void trainHam(String[] document) {
        for (String word : document) {
            hamWords++;
            ham.put(word, ham.containsKey(word) ? ham.get(word) + 1 : 1);
        }
    }

    private static void trainSpam(String[] document) {
        for (String word : document) {
            spamWords++;
            spam.put(word, spam.containsKey(word) ? spam.get(word) + 1 : 1);
        }
    }

    private static double ham(String word, double V) {
        double occurrence = ham.containsKey(word) ? ham.get(word) : 0;
        double noemer = occurrence + smoother;
        //chi-square vervanging
        if (noemer > top) {
            return 1;
        }
        double teller = hamWords + (smoother * V);
        double result = noemer / teller;
        return result;
    }

    private static double spam(String word, double V) {
        double occurrence = spam.containsKey(word) ? spam.get(word) : 0;
        double teller = spamWords + (smoother * V);
        double noemer = occurrence + smoother;
        //chi-square vervanging
        if (noemer > top) {
            return 1;
        }
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
