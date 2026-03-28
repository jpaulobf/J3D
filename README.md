# J3D - Java 3D Engine (Software & OpenGL)

O **J3D** é um motor de renderização 3D híbrido desenvolvido em Java. Ele possui uma arquitetura modular que suporta tanto um pipeline de renderização via **Software** (CPU puro) para fins educativos e nostálgicos, quanto um pipeline moderno via **OpenGL** (GPU) utilizando a biblioteca LWJGL.

## Arquitetura Modular

O projeto segue padrões de design modulares para facilitar a manutenção e expansão:

* **`core`**: O núcleo da aplicação. Contém o `Launcher`, a classe base `AbstractGame` (Game Loop) e interfaces centrais.
* **`player`**: Responsável pela lógica de controle do jogador (`PlayerController`), integrando input e física.
* **`sound`**: Gerencia a infraestrutura de áudio via OpenAL e decodificação de arquivos OGG, permitindo efeitos sonoros espaciais e feedback de movimento.
* **`render`**: Gerencia o pipeline de renderização (`SoftwareRenderer`), a `Camera`, o loop principal e os buffers (Imagem e Z-Buffer).
* **`math`**: Biblioteca matemática contendo `Matrix4` e `Transform` para cálculos de álgebra linear e transformações.
* **`geometry`**: Define as formas geométricas (`Mesh`), que podem ser geradas ou carregadas de arquivos, e suas primitivas de construção (`Vertex`, `Triangle`).
* **`lighting`**: Gerencia fontes de luz (`PointLight`) e materiais, aplicando cálculos de iluminação com base nas propriedades de superfície dos objetos (ex: cor difusa carregada de arquivos `.mtl`).
* **`io`**: Responsável pela leitura e parsing de arquivos externos, como modelos `.obj` e seus respectivos materiais `.mtl`.
* **`physics`**: Gerencia colisões híbridas (AABB para paredes e Mesh/Triângulos para chão), permitindo navegação realista em rampas, escadas e terrenos complexos.

## Renderizadores Disponíveis

### Software Renderer
Renderização clássica "old-school" feita inteiramente na CPU. Ideal para entender os fundamentos da Computação Gráfica (rasterização, projeção, clipping). Suporta SSAA e múltiplos algoritmos de rasterização.

### OpenGL Renderer
Renderização acelerada por hardware (GPU). Alcança performance extrema (4000+ FPS) com paridade visual completa em relação ao modo Software, incluindo atenuação de luz precisa e filtragem pixel-perfect.

> Consulte o arquivo `CHANGELOG.md` para o histórico detalhado de funcionalidades e detalhes técnicos.

## Comandos do Laboratório

| Categoria | Teclas | Função |
| :--- | :--- | :--- |
| **Movimento** | `W`, `S`, `A`, `D` | Navegação FPS (Frente, Trás, Strafe) |
| **Movimento** | `Espaço` | **Pular** |
| **Movimento** | `Caps Lock` | Alterna entre andar e **correr** |
| **Movimento** | `Scroll do Mouse` | Sobe e desce a câmera verticalmente |
| **Visão** | `Mouse` | Olhar ao redor (Yaw e Pitch) |
| **Luz (Debug)** | `J`, `L`, `U`, `O` | Move o spot de luz pelo plano horizontal |
| **Luz (Y)** | `I` / `K` | Sobe ou desce o spot de luz |
| **Modos** | `F2` | Alterna entre Preenchimento Sólido e **Wireframe** |
| **Modos** | `F3` | Mostra/Esconde a esfera da luz |
| **Modos** | `F4` | Alterna entre Flat Shading e **Gouraud Shading** |
| **Modos** | `F5` | Alterna o **SSAA 2x** (Anti-Aliasing) |
| **Modos** | `F10` | Alterna entre rasterização padrão e **Scanline** |
| **Interface** | `F6` | Mostra/Esconde o **HUD** (Mira e FPS) |
| **Sistema** | `F12` | Alterna entre **Tela Cheia** e Janela |
| **Sistema** | `ESC` | Fecha a aplicação |

## Como Executar

### Pré-requisitos
*   **Java 21** ou superior (Utiliza `Thread.onSpinWait` e Vector API).
*   **Maven** instalado.

### Comandos
1.  **Executar via Maven (Desenvolvimento):**
    ```bash
    mvn clean compile exec:exec
    ```

2.  **Gerar JAR e Executar:**
    ```bash
    mvn clean package
    java --add-modules=jdk.incubator.vector -jar target/j3d-engine-1.0-SNAPSHOT.jar
    ```

---
Desenvolvido como um laboratório de computação gráfica via Software.
