# CBO (acoplamento entre classes) por estado

- Data: 2026-10-26T09:59:48-03:00
- git version 2.55.0.windows.5; Maven (wrapper do projeto): 3.9.16; Java: openjdk version "17.0.20.1" 2026-08-18
- Ferramenta: CK 0.7.0 (com.github.mauricioaniche:ck:0.7.0, Maven Central)
- Comando por estado: `java -cp "<ck-lib>/*" com.github.mauricioaniche.ck.Runner <worktree>/prototype/src/main/java false 0 false <saida>/`
  (argumentos: diretório, useJars=false, maxFilesPerPartition=0 (automático), variablesAndFields=false, diretório de saída)
- Escopo: somente prototype/src/main/java. Com useJars=false, tipos externos (Spring, JDK) não são resolvidos por classpath; o CK os conta pelo nome declarado.
- O CK reporta classes internas (ex.: GeminiResponse$Candidate) como tipos próprios; elas entram nas contagens abaixo, e a coluna "Top-level" mostra só as de primeiro nível.
- Jars usados: checker-qual-3.8.0.jar ck-0.7.0.jar commons-csv-1.8.jar commons-io-2.10.0.jar commons-lang3-3.12.0.jar error_prone_annotations-2.5.1.jar failureaccess-1.0.1.jar guava-30.1.1-jre.jar j2objc-annotations-1.3.jar javassist-3.26.0-GA.jar jna-5.19.1.jar jna-platform-5.19.1.jar jsr305-3.0.2.jar listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar log4j-1.2.17.jar org.eclipse.core.commands-3.13.0.jar org.eclipse.core.contenttype-3.9.900.jar org.eclipse.core.expressions-3.9.700.jar org.eclipse.core.filesystem-1.11.500.jar org.eclipse.core.jobs-3.15.900.jar org.eclipse.core.resources-3.24.100.jar org.eclipse.core.runtime-3.35.0.jar org.eclipse.equinox.app-1.7.600.jar org.eclipse.equinox.common-3.21.0.jar org.eclipse.equinox.preferences-3.12.100.jar org.eclipse.equinox.registry-3.12.600.jar org.eclipse.jdt.core-3.26.0.jar org.eclipse.osgi-3.24.300.jar org.eclipse.text-3.14.800.jar org.osgi.service.prefs-1.1.2.jar osgi.annotation-8.0.1.jar reflections-0.9.12.jar slf4j-api-1.7.30.jar slf4j-log4j12-1.7.30.jar 
- Por classe: `metricas/cbo-por-classe.csv`

| Estado | Tipos (CK) | Top-level | CBO ResumoService | CBO médio | CBO máximo (classe) |
|---|---|---|---|---|---|
| baseline-acoplada | 18 | 13 | 16 | 2.72 | 16 (service.ResumoService) |
| cenario-1-acoplada | 16 | 13 | 15 | 2.75 | 15 (service.ResumoService) |
| cenario-2-acoplada | 26 | 18 | 22 | 2.65 | 22 (service.ResumoService) |
| cenario-3-acoplada | 20 | 13 | 18 | 2.70 | 18 (service.ResumoService) |
| baseline-desacoplada | 20 | 15 | 5 | 2.65 | 15 (provider.gemini.GeminiAdapter) |
| cenario-1-desacoplada | 18 | 15 | 5 | 2.67 | 14 (provider.groq.GroqAdapter) |
| cenario-2-desacoplada | 29 | 21 | 8 | 2.90 | 15 (provider.gemini.GeminiAdapter) |
| cenario-3-desacoplada | 22 | 15 | 5 | 2.64 | 17 (provider.gemini.GeminiAdapter) |
