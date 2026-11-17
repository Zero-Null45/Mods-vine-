# OreChop 0.1.0 — Minecraft Fabric 1.20.1

Primeiro prototipo **independente** inspirado na ideia de progressao por golpes do HT's TreeChop, aplicado a **veios de minerios**. Nenhuma classe, textura ou arquivo de HT's TreeChop e usado.

## Como funciona

- Ao minerar um minerio com a picareta certa, busca ate `maxVeinBlocks` blocos do **mesmo tipo**, conectados por faces (nao diagonais).
- O primeiro bloco quebra normalmente. Cada minerio minerado do mesmo veio conta como um golpe na estrutura.
- Quando o total minerado pelo jogador atinge `ceil(raiz_quadrada(N))`, onde N e a quantidade inicialmente detectada, o mod tenta quebrar os outros blocos do mesmo veio por meio do mecanismo de quebra vanilla.
- 1 bloco: 1 bloco normal. 2 blocos: 2 blocos normais. 4 blocos: 2 blocos e restante automatico. 9 blocos: 3 blocos e restante automatico. 16 blocos: 4 blocos e restante automatico.
- Nao mistura ferro com ouro, diamante com redstone etc. Variantes normais/deepslate do mesmo minerio contam como o mesmo tipo.
- Aceita minerios de outros mods cujo identificador termine em `_ore`.
- Ancient Debris incluido.
- Agachar enquanto minera desativa o efeito para aquele bloco.
- Efeito vale somente na quebra por jogador, com ferramenta capaz de coletar o minerio. Nao atua na quebra por TNT, pistoes ou qualquer outra origem.
- A rotina de quebra usa `ServerPlayerInteractionManager.tryBreakBlock()` para preservar as rotinas vanilla (loot, XP, ferramentas, eventos); incompatibilidades com outros mods de mineracao/protecao ainda precisam ser verificadas em jogo.

**Limite da versao 0.1.0:** ainda nao ha modelo 3D de minerio parcialmente lascado como existe no TreeChop; o feedback de progresso usa a actionbar. O progresso fica em memoria e expira apos 45 s sem quebrar blocos do veio. Nao ha persistencia entre reinicializacoes.

## Compatibilidade prevista

- Minecraft 1.20.1; Fabric Loader; Fabric API 0.92.12+1.20.1; Java 17.
- Pode ser instalado ao lado do HT's TreeChop, que trata arvores.
- **Desative/remova outros veinminers** (como VeinMiner) ao testar: dois mods quebrando veios ao mesmo tempo podem entrar em conflito.
- Funciona do lado servidor (tambem em singleplayer), sem telas nem teclas de cliente.

## Como gerar o .jar no GitHub (sem instalar Gradle)

1. Crie um repositorio novo no GitHub.
2. Envie **o conteudo desta pasta** para a raiz do repositorio, incluindo `.github/workflows/build.yml`.
3. Abra a aba **Actions** e selecione `Build OreChop (Fabric 1.20.1)`.
4. Execute **Run workflow** (ou aguarde o build disparado pelo push).
5. Abra a execucao concluida e baixe o artefato `OreChop-Fabric-1.20.1-JAR`.
6. Extraia `orechop-0.1.0.jar` e coloque em `mods/` da sua instancia Fabric 1.20.1.

Alternativamente, com Java 17 e Gradle 8.8 instalados, execute `gradle clean build`. O JAR sera gerado em `build/libs/`.

## Configuracoes

O mod cria `config/orechop.json` no primeiro inicio:

```json
{
  "enabled": true,
  "allowModdedOres": true,
  "sneakToMineNormally": true,
  "showActionBarProgress": true,
  "maxVeinBlocks": 64,
  "maxAxisDistance": 16,
  "sessionTimeoutSeconds": 45
}
```

- `maxVeinBlocks`: limite de 2 a 256 blocos.
- `maxAxisDistance`: limite de distancia por eixo em relacao ao primeiro bloco (2 a 32).
- `sessionTimeoutSeconds`: expira o progresso se voce parar de quebrar blocos do veio (5 a 600).
- Reinicie o jogo/servidor depois de editar.

## Estado / verificacao

O projeto foi preparado com as assinaturas publicadas para Yarn 1.20.1; **nao foi compilado nesta sessao**, pois as dependencias Minecraft/Fabric nao estao disponiveis localmente e o ambiente nao tem acesso a Maven. O workflow do GitHub executa a compilacao real. Trate esta versao como **experimental** e faca backup do mundo antes de usar.
