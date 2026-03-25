# Changelog

Todas as alterações notáveis neste projeto serão documentadas neste arquivo.

## [Unreleased] - OpenGL & Hybrid Update
### Adicionado
- **Renderizador OpenGL**: Implementação de um pipeline de renderização acelerado por hardware utilizando LWJGL e GLFW.
- **Sistema Híbrido**: Arquitetura desacoplada permitindo alternar entre renderização via Software (CPU) e Hardware (GPU) através da interface `IRenderer`.
- **Janela GLFW**: Suporte a janelas nativas modernas com controle de contexto OpenGL e V-Sync.
- **Fullscreen Toggle**: Alternância para tela cheia (Borderless/Exclusive) e modo janela com a tecla `F12`.
- **Input GLFW**: Abstração de entrada para suportar teclado e mouse via GLFW (fixação de cursor para FPS), mantendo compatibilidade com AWT.
- **Renderização de Sprites OpenGL**: Suporte a HUD e elementos 2D desenhados sobre a cena 3D no modo OpenGL.

### Refatoração e Arquitetura
- **AbstractGame**: Implementação do padrão *Template Method*, movendo o loop de jogo e gerenciamento de janela para uma classe base, deixando a classe `Game` focada apenas na lógica de cena.
- **Controllers**: Separação de responsabilidades com `PlayerController` (física e input do jogador) e `LightController` (gerenciamento de luzes e gizmos).
- **Factories**: Introdução de `WindowFactory` e `RenderFactory` para encapsular a criação de instâncias baseadas no `RenderType`.
- **Pacote Player**: Novo pacote `j3d.player` para organizar a lógica de controle de entidades.

## [1.0.0] - Software Renderer Foundation
### Funcionalidades (Legado - Detalhes Técnicos)
*(Funcionalidades originais implementadas no motor via Software)*

- **Frame Rate Independence**: Sistema de Delta Time para física e lógica desacopladas do FPS.
- **Object Pooling**: Reuso de vértices e matrizes para reduzir pressão no Garbage Collector.
- **Céu Procedural**: Gradiente vertical para simular atmosfera sem texturas.
- **Malhas Procedurais**: Geração de primitivas (Cubos, Esferas, Grids) via código.
- **Iluminação**: Modelos Flat e Gouraud Shading com reflexão difusa (Lambert).
- **Z-Buffering**: Resolução de visibilidade por pixel (Depth Buffer) e otimização 1/Z.
- **Carregador OBJ/MTL**: Parsing de modelos 3D e materiais com suporte a cores difusas.
- **Physics Engine**: Detecção de colisão AABB (Axis-Aligned Bounding Box) com gravidade e pulo.
- **Clipping**: Recorte de geometria no Near Plane para evitar artefatos visuais.
- **Frustum Culling**: Descarte de objetos fora do campo de visão da câmera usando esferas.
- **HUD 2D**: Sistema de interface para desenho de sprites e texto sobre a cena 3D.
- **Otimizações CPU**: Rasterização incremental (apenas somas no loop interno) e multithreading no SSAA.
- **Anti-Aliasing**: SSAA 2x (Super Sampling) para suavização de bordas no modo Software.
- **Rasterização Híbrida (Software)**: Alternância em tempo real entre algoritmos Baricêntrico e Scanline.
