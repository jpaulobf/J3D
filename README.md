# 🚀 J3D - Java Software Renderer (Modular v9.1)

O **J3D** é um motor de renderização 3D de alta performance processado inteiramente via software. Ao contrário de engines modernas que dependem de GPUs e APIs como OpenGL ou Vulkan, o J3D realiza todos os cálculos matemáticos de álgebra linear, projeção de perspectiva e rasterização de pixels diretamente na **CPU** utilizando **Java puro**.

## 🏗️ Arquitetura Modular

O projeto segue padrões de design modulares para facilitar a manutenção e expansão:

* **`Launcher`**: O ponto de entrada. Inicializa a janela (JFrame) e a thread de renderização.
* **`renderer`**: Contém o `SoftwareRenderer`. Gerencia o loop principal, o **Buffer de Imagem** e o **Z-Buffer**.
* **`game`**: Define o `GameObject`, que encapsula a geometria (`Mesh`) e a lógica de renderização (Rasterização e Wireframe).
* **`geometry`**: A fundação matemática do sistema.
    * `Matrix4`: Matrizes 4x4 para transformações espaciais.
    * `Mesh`: Fábricas de formas geométricas (Cubo, Pirâmide, Esfera, Grid).
    * `Vertex` / `Triangle`: Primitivas fundamentais de construção.
* **`camera`**: Gerencia o ponto de vista do observador e calcula a **View Matrix**.
* **`matriz`**: Contém o `Transform`, responsável por gerenciar translação e rotação (Model Matrix).
* **`light`**: Define fontes de luz pontuais (`PointLight`).

## 🛠️ Destaques Técnicos

### 1. Sistema de Target FPS
O motor possui um controle dinâmico de taxa de quadros (`TARGET_FPS`). Implementamos um fator de correção (`speedCorrection`) que normaliza o movimento com base em um padrão de 60 FPS (16ms), garantindo consistência física independente da fluidez visual.

### 2. Iluminação Lambertiana
Utilizamos sombreamento difuso para calcular a interação da luz com as faces:
* **Atenuação de Distância**: A intensidade da luz diminui conforme se afasta da fonte.
* **Produto Escalar (Dot Product)**: Determina o brilho com base no ângulo entre a normal da face e o vetor da luz.

### 3. Z-Buffering (Depth Buffer)
Um buffer de profundidade armazena a distância de cada pixel, resolvendo o problema de visibilidade e garantindo que objetos próximos cubram corretamente os distantes.

## 🎮 Comandos do Laboratório

| Categoria | Teclas | Função |
| :--- | :--- | :--- |
| **Movimento** | `W`, `S`, `A`, `D` | Navegação FPS (Frente, Trás, Strafe) |
| **Visão** | `Mouse` | Olhar ao redor (Yaw e Pitch) |
| **Luz (X, Z)** | `Setas Direcionais` | Move o spot de luz pelo plano horizontal |
| **Luz (Y)** | `I` / `K` | Sobe ou desce o spot de luz |
| **Modos** | `F2` | Alterna entre Preenchimento Sólido e **Wireframe** |
| **Gizmo** | `F3` | Mostra/Esconde a esfera da luz |

## 🚀 Como Executar

1. Compile a partir da pasta `src`:
   `javac Launcher.java camera/*.java game/*.java geometry/*.java light/*.java matriz/*.java renderer/*.java`
2. Execute:
   `java Launcher`

---
Desenvolvido como um laboratório de computação gráfica via Software.
