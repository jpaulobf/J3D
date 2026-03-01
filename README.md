# J3D - Java Software Renderer

O **J3D** é um motor de renderização 3D de alta performance processado inteiramente via software. Ao contrário de engines modernas que dependem de GPUs e APIs como OpenGL ou Vulkan, o J3D realiza todos os cálculos matemáticos de álgebra linear, projeção de perspectiva e rasterização de pixels diretamente na **CPU** utilizando **Java puro**.

## Arquitetura Modular

O projeto segue padrões de design modulares para facilitar a manutenção e expansão:

* **`core`**: O núcleo da aplicação. Contém o `Launcher` (ponto de entrada) e o `GameObject`, que encapsula a lógica dos objetos.
* **`render`**: Gerencia o pipeline de renderização (`SoftwareRenderer`), a `Camera`, o loop principal e os buffers (Imagem e Z-Buffer).
* **`math`**: Biblioteca matemática contendo `Matrix4` e `Transform` para cálculos de álgebra linear e transformações.
* **`geometry`**: Define as formas geométricas (`Mesh`), que podem ser geradas ou carregadas de arquivos, e suas primitivas de construção (`Vertex`, `Triangle`).
* **`lighting`**: Gerencia fontes de luz (`PointLight`) e materiais, aplicando cálculos de iluminação com base nas propriedades de superfície dos objetos (ex: cor difusa carregada de arquivos `.mtl`).
* **`io`**: Responsável pela leitura e parsing de arquivos externos, como modelos `.obj` e seus respectivos materiais `.mtl`.
* **`physics`**: Módulo de física responsável pela detecção de colisão entre entidades.

## Destaques Técnicos

### 1. Frame Rate Independence (Delta Time)
O motor utiliza um sistema de **Delta Time** para desacoplar a lógica do jogo da taxa de quadros. Isso garante que a velocidade de movimento e a física sejam consistentes, independentemente se o jogo está rodando a 30, 60 ou 144 FPS.

### 2. Iluminação Dinâmica (Flat & Gouraud)
O motor implementa o modelo de reflexão difusa (Lambertiana) com suporte a dois modos de sombreamento alternáveis em tempo real:
* **Flat Shading**: A luz é calculada uma vez por face, resultando em um visual facetado.
* **Gouraud Shading**: A luz é calculada por vértice e interpolada através do triângulo, criando superfícies suaves.

### 3. Z-Buffering (Depth Buffer)
Um buffer de profundidade armazena a distância de cada pixel, resolvendo o problema de visibilidade e garantindo que objetos próximos cubram corretamente os distantes.

### 4. Parser de Modelos .obj e .mtl
O motor agora é capaz de carregar modelos 3D a partir de arquivos **Wavefront (.obj)**. O parser integrado extrai vértices, normais e faces do modelo. Além disso, há suporte para arquivos de materiais **(.mtl)**, permitindo que cada objeto tenha suas próprias propriedades de superfície, como a cor difusa (`Kd`), que são aplicadas durante a renderização.

### 5. Motor de Física e Detecção de Colisão
O J3D inclui um `PhysicsEngine` que implementa detecção de colisão AABB (Axis-Aligned Bounding Box). O sistema trata colisões entre o jogador e o cenário, e agora também entre **objetos dinâmicos** (ex: carro colidindo com obstáculos), impedindo sobreposições físicas.

### 6. Clipping de Projeção (Near Plane Clipping)
Para corrigir artefatos visuais e evitar a renderização de geometria que está atrás da câmera, foi implementado um sistema de clipping simples no plano próximo (near plane). Triângulos que cruzam ou estão atrás deste plano são descartados antes da rasterização, melhorando a performance e a correção visual.

### 7. Frustum Culling (Descarte de Objetos)
Para permitir cenários vastos com muitos elementos, o motor implementa um teste de visibilidade baseado em esferas. Antes de processar a geometria de um objeto, verifica-se se sua esfera envolvente está dentro do campo de visão da câmera. Objetos atrás ou muito distantes são ignorados antes mesmo de entrarem no pipeline de renderização, economizando processamento.

### 8. Sistema de HUD (Interface 2D)
Foi introduzido um sistema de renderização de sprites 2D (bitmaps) que opera sobre a camada 3D final. Isso permite desenhar elementos de interface como miras (crosshair) e textos (fontes bitmap) com transparência, essenciais para feedback visual ao jogador (ex: FPS, Mira).

### 9. Otimizações de Performance na CPU
Foram aplicadas diversas otimizações de baixo nível para maximizar o FPS em um ambiente de renderização por software:
*   **Rasterização Incremental**: O loop de desenho de pixels foi reescrito para usar apenas somas (algoritmo incremental), removendo todas as multiplicações e divisões por pixel.
*   **1/Z Buffering**: O buffer de profundidade armazena o inverso de Z (`1/Z`), permitindo interpolação linear sem divisões custosas.
*   **Multithreading**: O passo de resolução do SSAA é paralelizado para utilizar todos os núcleos da CPU.
*   **Backface Culling Otimizado**: A verificação de faces traseiras agora é feita antes do cálculo da normalização (raiz quadrada), economizando ciclos de CPU.
*   **Pré-cálculo de Luzes**: A posição das luzes no espaço da câmera é calculada apenas uma vez por objeto, em vez de repetidamente para cada vértice.

### 10. Anti-Aliasing (SSAA 2x)
Implementação de **Super Sampling Anti-Aliasing (SSAA)**. O renderizador desenha a cena em uma resolução 4x maior (2x largura, 2x altura) e faz uma amostragem (downsampling) para a resolução da tela, suavizando as bordas serrilhadas.

### 11. Geração Procedural de Cenário (Maze)
O motor inclui um exemplo de geração procedural de labirintos baseada em grid. O cenário atual cria paredes, chão e teto dinamicamente, otimizando o uso de memória através do reuso de malhas (Instancing) para desenhar centenas de blocos com baixo custo de CPU.

### 12. Interface Escalável (Responsive HUD)
O sistema de HUD foi atualizado para ser independente de resolução. A mira e os textos se adaptam automaticamente ao tamanho da janela (ex: Full HD), mantendo a proporção visual correta e legibilidade em qualquer display.

### 13. Rasterização Híbrida (Scanline vs. Baricêntrica)
O motor agora suporta dois algoritmos de rasterização que podem ser alternados em tempo real: o método padrão baseado em coordenadas baricêntricas e um rasterizador clássico por **Scanline**. Isso permite a comparação de performance e o estudo de diferentes técnicas de preenchimento de polígonos, com indicadores visuais no HUD para cada modo.

## Comandos do Laboratório

| Categoria | Teclas | Função |
| :--- | :--- | :--- |
| **Movimento** | `W`, `S`, `A`, `D` | Navegação FPS (Frente, Trás, Strafe) |
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
| **Sistema** | `ESC` | Fecha a aplicação |

## Como Executar

1. Compile a partir da pasta `src`:
   `javac core/*.java geometry/*.java lighting/*.java math/*.java render/*.java physics/*.java io/*.java`
2. Execute:
   `java core.Launcher`

---
Desenvolvido como um laboratório de computação gráfica via Software.
