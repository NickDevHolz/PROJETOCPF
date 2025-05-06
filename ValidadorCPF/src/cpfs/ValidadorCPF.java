package cpfs;

import java.io.*;
import java.util.*;

public class ValidadorCPF {
    public static void main(String[] args) throws Exception {
        int[] quantidadesThreads = {1, 2, 3, 5, 6, 10, 15, 30};

        File pasta = new File("cpfs");
        File[] arquivos = pasta.listFiles((dir, name) -> name.endsWith(".txt"));
        if (arquivos == null || arquivos.length == 0) {
            System.out.println("Nenhum arquivo .txt encontrado na pasta 'cpfs'.");
            return;
        }

        Arrays.sort(arquivos);

        for (int qtdThreads : quantidadesThreads) {
            System.out.println("Executando com " + qtdThreads + " thread(s)");

            int[] resultado = new int[2]; // [0] = válidos, [1] = inválidos

            long inicio = System.currentTimeMillis();

            List<Thread> threads = new ArrayList<>();
            int tamanho = arquivos.length / qtdThreads;

            for (int i = 0; i < qtdThreads; i++) {
                int inicioIndice = i * tamanho;
                int fimIndice = (i == qtdThreads - 1) ? arquivos.length : (i + 1) * tamanho;

                List<File> subLista = Arrays.asList(Arrays.copyOfRange(arquivos, inicioIndice, fimIndice));
                Thread t = new Thread(new CPFProcessor(subLista, resultado));
                threads.add(t);
                t.start();
            }

            for (Thread t : threads) {
                t.join();
            }

            long fim = System.currentTimeMillis();

            System.out.println("Válidos: " + resultado[0]);
            System.out.println("Inválidos: " + resultado[1]);
            System.out.println("Tempo de execução (ms): " + (fim - inicio));
            System.out.println("--------------------------------------");

            try (PrintWriter out = new PrintWriter("versao_" + qtdThreads + "_threads.txt")) {
                out.println("Válidos: " + resultado[0]);
                out.println("Inválidos: " + resultado[1]);
                out.println("Tempo de execução (ms): " + (fim - inicio));
            }
        }
    }
}

class CPFProcessor implements Runnable {
    private List<File> arquivos;
    private int[] resultado;

    public CPFProcessor(List<File> arquivos, int[] resultado) {
        this.arquivos = arquivos;
        this.resultado = resultado;
    }

    @Override
    public void run() {
        int validos = 0;
        int invalidos = 0;
        for (File arquivo : arquivos) {
            try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
                String linha;
                while ((linha = br.readLine()) != null) {
                    if (CPFValidator.validaCPF(linha)) validos++;
                    else invalidos++;
                }
            } catch (IOException e) {
                System.out.println("Erro lendo arquivo: " + arquivo.getName());
            }
        }
        synchronized (resultado) {
            resultado[0] += validos;
            resultado[1] += invalidos;
        }
    }
}

class CPFValidator {
    public static boolean validaCPF(String cpf) {
        cpf = cpf.replaceAll("\\D", "");
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) return false;

        for (int i = 9; i <= 10; i++) {
            int soma = 0;
            for (int j = 0; j < i; j++) {
                soma += (cpf.charAt(j) - '0') * ((i + 1) - j);
            }
            int digito = (soma * 10) % 11;
            if (digito == 10) digito = 0;
            if (digito != (cpf.charAt(i) - '0')) return false;
        }
        return true;
    }
}
