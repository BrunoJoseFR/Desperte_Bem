Criação por: 
Argeu Piai
Beatriz Macedo Mollica
Brunna Pinheiro
Bruno José Ferreira Ribeiro
Ricardo H. Jr. K. Lopes
Victoria Macedo Mollica

Desperte Bem é um aplicativo Android desenvolvido em Kotlin + Jetpack Compose que permite ao usuário:
  * definir um horário para um alarme;
  * gravar o nível de ruído ambiente até o momento do alarme;
  * exibir uma animação de onda em tempo real durante a gravação;
  * mostrar um gráfico final com os decibéis capturados.
   
O objetivo é criar um despertador inteligente que monitora o ambiente antes do alarme tocar.
Para seu funcionamento, ele emprega as seguintes tecnologias:
  * Kotlin;
  * Jetpack Compose;
  * MediaRecorder (captura de áudio);
  * Vico Chart Library (gráficos e animação de onda);
  * AlarmManager (agendamento de alarmes);
  * Activity Result API (permissões).

O app é composto por três telas principais:
  1. Tela de Configuração do Alarme
  Permite ao usuário escolher hora e minuto usando um TimePicker.
  Fluxo:
    * usuário escolhe o horário;
    * o app agenda um alarme com AlarmManager;
    * se a permissão de microfone estiver liberada, inicia a gravação;
    * caso contrário, solicita permissão.

  3. Tela de Gravação (BlankRecordingScreen)
  Durante a gravação:
    * o app usa MediaRecorder para capturar o áudio ambiente;
    * a cada 200 ms, lê maxAmplitude;
    * converte amplitude em decibéis.
   Atualiza:
      * uma lista completa (samples);
      * uma lista curta para animação (liveEntries).
  A animação é exibida com kotlin, através do seguinte script:
    Chart(
        chart = lineChart(),
        model = entryModelOf(*liveEntries.toTypedArray())
    )
  Essa é a onda animada em tempo real.

  5. Tela de Gráfico Final (GraphScreen)
  Após o alarme ou ao tocar em “Skip”, o app exibe:
    * um gráfico de linha com todos os decibéis capturados;
    * botão para reiniciar o processo.

Para a captura de áudio, o app utiliza Kotlin da seguinte forma:
  mediaRecorder.maxAmplitude
Esse valor é convertido para decibéis:
  20 * log10(amplitude)
Esses valores alimentam:
  * a animação ao vivo;
  * o gráfico final.

A animação de onda em tempo real é gerada pela biblioteca Vico, atualizada a cada 200 ms.
A lista liveEntries mantém apenas os últimos 60 valores para garantir fluidez:
  if (liveEntries.size > 60) {
      liveEntries.removeAt(0)
  }
Isso cria uma onda contínua e leve.

O agendamento do alarme é configurado com:
  alarmManager.setExactAndAllowWhileIdle(...)
O app também verifica se o usuário permitiu alarmes exatos.

Para funcionamento, o app solicita algumas permissões necessárias:
  * RECORD_AUDIO — para capturar o som ambiente;
  * Permissão para alarmes exatos (Android 12+).
  
Como Executar o Projeto
  * abra o projeto no Android Studio;
  * conecte um dispositivo físico (recomendado);
  * clique em Run;
  * defina um horário;
  * permita o uso do microfone;
  * observe a animação da onda durante a gravação.

Limitações:
  * o emulador Android não captura áudio real, então a onda pode ficar estática.
  * para testar corretamente, use um celular físico.

Arquivos Importantes
  * MainActivity.kt — contém toda a lógica do app
  * AlarmReceiver.kt — recebe o alarme (não enviado aqui)
  * build.gradle.kts — inclui dependências do Compose e Vico

Melhorias futuras:
  * adicionar sons personalizados para o alarme;
  * criar histórico de gravações;
  * exportar gráficos;
  * detectar padrões de ruído.
