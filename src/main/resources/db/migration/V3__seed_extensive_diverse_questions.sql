-- =============================================================================
-- Migration: V3__seed_extensive_diverse_questions.sql
-- Descrição: Carga Massiva de Questões e Flashcards Multidisciplinares (Medicina e SI)
--            com Fundamentações Clínicas e Técnicas Aprofundadas para Validação
--            do Motor de Repetição MMEEBB e do Pipeline de RAG (pgvector).
-- Autor: Níckolas Tavares / Projeto TCC UNIPAM
-- =============================================================================

-- =============================================================================
-- 1. MEDICINA — 1. CLÍNICA MÉDICA (subject_id = 1)
-- =============================================================================

INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    1,
    'Cardiologia - Síndrome Coronariana Aguda',
    'MULTIPLE_CHOICE',
    'Homem de 58 anos dá entrada na Unidade de Pronto Atendimento com dor torácica retroesternal opressiva há 45 minutos, irradiada para mandíbula e membro superior esquerdo, associada a diaforese fria. O eletrocardiograma (ECG) inicial evidencia supradesnivelamento do segmento ST de 3 mm nas derivações DII, DIII e aVF. O hospital de referência com serviço de hemodinâmica situa-se a 40 minutos de distância. Qual é a conduta prioritária e o tempo-meta preconizado pela Diretriz da SBC?',
    'A',
    '["A) Encaminhamento imediato para Angioplastia Coronária Transluminal Primária com tempo porta-balão alvo menor que 120 minutos", "B) Realização imediata de trombólise química na UPA com Tenecteplase e transferência apenas se houver falha de reperfusão", "C) Solicitação de curva seriada de troponina ultrassensível para confirmação diagnóstica antes de qualquer decisão de reperfusão", "D) Aguardar 6 horas na sala de emergência para avaliar se ocorre regressão espontânea do segmento ST com nitrato sublingual"]'::jsonb,
    'Trata-se de um Infarto Agudo do Miocárdio com Supradesnivelamento do Segmento ST (IAMCSST) de parede inferior. Pela Diretriz Brasileira da SBC, a angioplastia primária é a estratégia de escolha quando o tempo estimado entre o primeiro contato médico e o balão for <= 120 minutos (inclusive considerando o transporte de 40 min). Caso a previsão superasse 120 minutos, a fibrinólise deveria ser realizada em até 10 minutos (tempo porta-agulha). A troponina não deve atrasar a reperfusão.',
    'HARD',
    'Diretriz SBC / ENARE 2024',
    TRUE
),
(
    1,
    'Cardiologia - Fibrilação Atrial',
    'MULTIPLE_CHOICE',
    'Mulher de 73 anos, hipertensa em uso de enalapril e diabética em uso de metformina, sem histórico de AVC ou doença vascular, apresenta Fibrilação Atrial (FA) paroxística confirmada por Holter. Na avaliação pelo escore CHA2DS2-VASc, qual é a pontuação da paciente e a respectiva recomendação de anticoagulação?',
    'C',
    '["A) Escore 2; não há indicação de anticoagulação formal, sendo recomendado apenas Ácido Acetilsalicílico (AAS)", "B) Escore 3; anticoagulação com Varfarina mantendo INR entre 1.5 e 2.0", "C) Escore 4; recomendada anticoagulação oral plena, preferencialmente com Anticoagulantes Orais Diretos (DOACs)", "D) Escore 5; indicação exclusiva de ablação cirúrgica do apêndice atrial esquerdo sem medicação"]'::jsonb,
    'Pontuação no CHA2DS2-VASc: Idade entre 65-74 anos (+1), Sexo feminino (+1), Hipertensão arterial (+1) e Diabetes mellitus (+1) = Total de 4 pontos. Para mulheres com escore >= 3 (ou homens >= 2), a diretriz da SBC/ESC recomenda anticoagulação oral plena. Os DOACs (Apixabana, Rivaroxabana, Dabigatrana ou Edoxabana) são a primeira linha de preferência em relação aos antagonistas da vitamina K por menor risco de hemorragia intracraniana.',
    'MEDIUM',
    'Diretrizes SBC / Revalida INEP',
    TRUE
),
(
    1,
    'Cardiologia - Emergência Hipertensiva',
    'MULTIPLE_CHOICE',
    'Paciente de 45 anos chega ao pronto-socorro confuso e sonolento, com PA de 220x130 mmHg. À fundoscopia, observa-se papiledema bilateral e hemorragias em chama de vela. Não há déficits motores focais. Como é classificado o quadro clínico e qual a conduta anti-hipertensiva imediata?',
    'B',
    '["A) Urgência hipertensiva; administrar Captopril 25 mg por via sublingual e liberar para casa após redução para PA normal", "B) Emergência hipertensiva (Encefalopatia Hipertensiva); internação em UTI e infusão contínua de Nitroprussiato de Sódio intravenoso com meta de redução gradual da PA média em 20-25% na primeira hora", "C) Pseudo-crise hipertensiva; prescrever Diazepam oral e analgesia simples com observação", "D) Acidente Vascular Cerebral Isquêmico agudo; manter a pressão arterial elevada e contraindicar qualquer anti-hipertensivo"]'::jsonb,
    'A presença de PA marcadamente elevada associada à lesão aguda e progressiva de órgão-alvo (encefalopatia hipertensiva comprovada por papiledema e alteração de nível de consciência) configura Emergência Hipertensiva. O tratamento exige internação em leito de UTI e drogas parenterais tituláveis (ex: Nitroprussiato de Sódio intravenoso). A meta de redução da PAM é de 20% a 25% na 1ª hora para prevenir isquemia cerebral reflexa.',
    'HARD',
    'Diretrizes SBC de Hipertensão Arterial',
    TRUE
),
(
    1,
    'Endocrinologia - Cetoacidose Diabética',
    'MULTIPLE_CHOICE',
    'Jovem de 19 anos, com Diabetes Mellitus tipo 1, dá entrada no pronto-socorro com náuseas, vômitos, dor abdominal difusa, taquipneia profunda (respiração de Kussmaul) e hálito cetônico. Exames laboratoriais: Glicemia = 420 mg/dL, pH arterial = 7.15, Bicarbonato = 10 mEq/L, Ânion Gap = 22, Potássio sérico (K+) = 3.1 mEq/L. Qual é a conduta farmacológica prioritária em relação à insulinoterapia?',
    'C',
    '["A) Iniciar imediatamente bólus de insulina regular intravenosa (0.1 UI/kg) antes de qualquer outra medida", "B) Prescrever bicarbonato de sódio intravenoso imediatamente devido ao pH menor que 7.20", "C) Suspender/adiar o início da insulinoterapia e realizar reposição vigorosa de cloreto de potássio até que o K+ atinja valor superior a 3.3 mEq/L", "D) Administrar insulina glargina subcutânea na dose diária habitual e manter o paciente em jejum"]'::jsonb,
    'Na Cetoacidose Diabética (CAD), a insulina promove a entrada maciça de potássio para dentro das células. Se a insulinoterapia for iniciada com potássio sérico < 3.3 mEq/L, o paciente corre risco iminente de hipocalemia grave, arritmias ventriculares fatais e parada cardiorrespiratória. Portanto, a regra fundamental de segurança da SBD e da ADA é adiar a insulina e repor potássio (20-30 mEq/h) até K+ > 3.3 mEq/L.',
    'HARD',
    'SBD 2024 / ADA Standards of Care',
    TRUE
),
(
    1,
    'Endocrinologia - Hipotireoidismo',
    'FLASHCARD',
    'Qual é o perfil laboratorial característico do Hipotireoidismo Primário manifesto e qual anticorpo sérico é o principal marcador da Tireoidite de Hashimoto?',
    'TSH elevado com T4 livre reduzido; o principal marcador etiológico é o anticorpo anti-peroxidase tireoidiana (anti-TPO).',
    NULL,
    'No hipotireoidismo primário, a falência da glândula tireoide leva à queda de T4 livre, que por retroalimentação negativa (feedback negativo) no eixo hipotálamo-hipófise estimula a hipersecreção de TSH. A etiologia autoimune mais frequente no Brasil e no mundo é a Tireoidite Crônica de Hashimoto, evidenciada pela positividade dos títulos de anti-TPO em mais de 90% dos pacientes.',
    'MEDIUM',
    'Consenso SBEM de Hipotireoidismo',
    TRUE
),
(
    1,
    'Nefrologia - Doença Renal Crônica',
    'MULTIPLE_CHOICE',
    'Homem de 65 anos, hipertenso e diabético de longa data, apresenta creatinina sérica de 1.8 mg/dL (Taxa de Filtração Glomerular estimada pela equação CKD-EPI de 38 mL/min/1.73m²) e relação albuminúria/creatininúria urinária (RAC) de 420 mg/g mantidas por mais de 4 meses. De acordo com o KDIGO 2024, qual é o estadiamento da DRC do paciente e qual classe medicamentosa demonstrou nefroproteção com redução de progressão da doença?',
    'B',
    '["A) Estágio G2 A1; bloqueadores dos canais de cálcio di-hidropiridínicos em altas doses", "B) Estágio G3b A3; inibidores do cotransportador sódio-glicose 2 (iSGLT2) e bloqueadores do sistema renina-angiotensina (IECA/BRA)", "C) Estágio G4 A2; indicação imediata de diálise peritoneal e suspensão de anti-hipertensivos", "D) Estágio G1 A3; prescrição de anti-inflamatórios não-esteroidais para controle da inflamação renal"]'::jsonb,
    'Pelos critérios KDIGO, a TFG entre 30 e 44 mL/min/1.73m² corresponde ao estágio G3b, e a albuminúria > 300 mg/g corresponde à categoria A3 (gravemente aumentada). As diretrizes atuais preconizam o bloqueio do SRAA (IECA ou BRA) e a introdução de iSGLT2 (Dapagliflozina ou Empagliflozina), que comprovadamente reduzem a pressão intraglomerular, a perda de função renal e desfechos cardiovasculares.',
    'HARD',
    'KDIGO 2024 / SBN',
    TRUE
),
(
    1,
    'Infectologia - Sepse e Choque Séptico',
    'MULTIPLE_CHOICE',
    'Pelos critérios do consenso internacional Sepsis-3, qual achado define a transição de um quadro de sepse para Choque Séptico?',
    'A',
    '["A) Hipotensão arterial persistente com necessidade de vasopressores para manter PAM >= 65 mmHg e lactato sérico > 2 mmol/L (18 mg/dL) apesar de ressuscitação volêmica adequada", "B) Presença de febre alta > 39°C associada a leucocitose > 15.000 com desvio à esquerda", "C) Pontuação no escore qSOFA maior ou igual a 2 na triagem da emergência", "D) Necessidade de ventilação mecânica invasiva nas primeiras 24 horas de internação"]'::jsonb,
    'Segundo a definição do Sepsis-3 (Surviving Sepsis Campaign), o choque séptico é um subgrupo da sepse em que anormalidades circulatórias e celulares/metabólicas são profundas o suficiente para aumentar substancialmente a mortalidade. É clinicamente identificado pela necessidade de vasopressor para manter PAM >= 65 mmHg E nível de lactato sérico > 2 mmol/L após ressuscitação com fluidos.',
    'HARD',
    'Sepsis-3 Guidelines / ILAS',
    TRUE
),
(
    1,
    'Pneumologia - Tromboembolismo Pulmonar',
    'MULTIPLE_CHOICE',
    'Mulher de 32 anos, usuária de anticoncepcional oral combinado, procura atendimento com dor torácica pleurítica súbita e dispneia há 6 horas. Exame físico: FC = 112 bpm, FR = 24 irpm, SpO2 = 91% em ar ambiente. Membro inferior esquerdo com edema e empastamento de panturrilha. Pelo Escore de Wells, a paciente é classificada com alta probabilidade clínica de TEP. Qual é o exame padrão-ouro de imagem para confirmação diagnóstica?',
    'B',
    '["A) Radiografia simples de tórax em duas posições buscando o Sinal de Westermark", "B) Angiotomografia computadorizada de tórax com contraste venoso", "C) Dosagem quantitativa de D-dímero por método ELISA de alta sensibilidade", "D) Eletrocardiograma buscando o padrão S1Q3T3"]'::jsonb,
    'Em pacientes com probabilidade clínica intermediária ou alta pelo escore de Wells (a paciente pontua por sinais de TVP +3, taquicardia +1.5 e ausência de diagnóstico alternativo mais provável +3), o D-dímero não deve ser utilizado para afastar a doença. O método padrão-ouro de escolha não invasivo com alta sensibilidade e especificidade para visualização de falhas de enchimento vascular é a Angiotomografia Computadorizada de Tórax.',
    'MEDIUM',
    'Diretrizes SBPT / ESC',
    TRUE
),
(
    1,
    'Pneumologia - Pneumonia Adquirida na Comunidade',
    'FLASHCARD',
    'Quais são os 5 parâmetros avaliados no escore CURB-65 para estratificação de risco de mortalidade e decisão de internação na Pneumonia Adquirida na Comunidade (PAC)?',
    'C: Confusão mental aguda; U: Ureia sérica >= 50 mg/dL (ou BUN > 19 mg/dL); R: Frequência respiratória >= 30 irpm; B: Pressão arterial baixa (PAS < 90 mmHg ou PAD <= 60 mmHg); 65: Idade >= 65 anos.',
    NULL,
    'O escore CURB-65 auxilia na decisão do local de tratamento na PAC: escore 0 ou 1 indica tratamento ambulatorial seguro; escore 2 sugere internação em enfermaria geral; escore >= 3 orienta internação hospitalar com consideração de UTI se houver choque ou necessidade de suporte ventilatório mecânico.',
    'MEDIUM',
    'Diretriz Brasileira de PAC / SBPT',
    TRUE
),
(
    1,
    'Hematologia - Diagnóstico Diferencial de Anemias',
    'MULTIPLE_CHOICE',
    'Mulher de 28 anos, com queixa de fadiga crônica e menorragia. Hemograma: Hemoglobina = 9.2 g/dL, VCM = 71 fL (microcítica), HCM = 23 pg (hipocrômica), RDW = 18.5% (anisocitose acentuada). Qual perfil do perfil de ferro confirma o diagnóstico de Anemia Ferropriva?',
    'D',
    '["A) Ferritina sérica elevada, Ferro sérico normal e Capacidade Total de Ligação do Ferro (CTLF) diminuída", "B) Ferritina normal com saturação de transferrina superior a 50%", "C) Depósitos de ferro normais na medula óssea e presença de corpúsculos de Howell-Jolly", "D) Ferritina sérica baixa (< 30 ng/mL), Ferro sérico reduzido e Capacidade Total de Ligação do Ferro (CTLF) elevada"]'::jsonb,
    'A anemia ferropriva clássica decorrente de perda sanguínea apresenta carência total de estoques férricos: ferritina sérica baixa (< 30 ng/mL, o marcador mais específico), ferro sérico reduzido e CTLF/Transferrina elevada (tentativa compensatória do fígado de captar mais ferro), gerando índice de saturação da transferrina < 15-20% e anisocitose (RDW alto).',
    'EASY',
    'ABHH / Harrison Medicina Interna',
    TRUE
);

-- =============================================================================
-- 2. MEDICINA — 2. CIRURGIA GERAL (subject_id = 2)
-- =============================================================================

INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    2,
    'Abdome Agudo Inflamatório - Colecistite Aguda',
    'MULTIPLE_CHOICE',
    'Mulher de 48 anos, multípara, procura o pronto atendimento com dor contínua em hipocôndrio direito de início após refeição gordurosa há 14 horas, associada a febre de 38.3°C, vômitos e leucocitose. Ao exame, apresenta interrupção súbita da inspiração profunda durante a palpação profunda do rebordo costal direito. Qual o nome deste sinal semiológico e a conduta cirúrgica definitiva preconizada pelo consenso de Tóquio (Tokyo Guidelines)?',
    'A',
    '["A) Sinal de Murphy; indicação de Colecistectomia Videolaparoscópica precoce (idealmente nas primeiras 72 horas do início dos sintomas)", "B) Sinal de Rovsing; laparotomia mediana ampla para drenagem de abscessos pélvicos", "C) Sinal de Cullen; conduta expectante com alta hospitalar e analgésicos comuns por via oral", "D) Sinal de Kehr; esplenectomia de urgência por suspeita de rotura esplênica secundária"]'::jsonb,
    'O sinal de Murphy (parada inspiratória à palpação do ponto cístico) é o clássico sinal semiológico da Colecistite Aguda Calculosa. Conforme os critérios das Tokyo Guidelines (TG18), o tratamento de escolha para pacientes com colecistite aguda graus I e II com condições cirúrgicas é a colecistectomia videolaparoscópica precoce realizada nas primeiras 72 horas a 7 dias, reduzindo custos e complicações.',
    'MEDIUM',
    'Tokyo Guidelines TG18 / Sabiston',
    TRUE
),
(
    2,
    'Abdome Agudo Inflamatório - Diverticulite Aguda',
    'MULTIPLE_CHOICE',
    'Homem de 62 anos, com dor progressiva em fossa ilíaca esquerda há 3 dias e febre baixa. A tomografia computadorizada de abdome e pelve revela espessamento parietal do cólon sigmoide associado a abscesso pericólico localizado de 2.5 cm, sem ar livre intraperitoneal (Classificação de Hinchey I). Qual é a conduta terapêutica inicial recomendada?',
    'C',
    '["A) Cirurgia de urgência com colectomia a Hartmann com colostomia terminal imediata", "B) Colonoscopia diagnóstica de urgência para lavagem e biópsia da mucosa diverticular", "C) Tratamento conservador com repouso intestinal relativo, hidratação intravenosa e antibioticoterapia sistêmica de amplo espectro com cobertura para gram-negativos entéricos e anaeróbios", "D) Drenagem percutânea do abscesso guiada por tomografia computadorizada"]'::jsonb,
    'Na diverticulite aguda não complicada ou Hinchey Ia/Ib com pequenos abscessos pericólicos (< 3-4 cm), a conduta padrão inicial é clínica/conservadora, com antibioticoterapia cobrindo anaeróbios (ex: Bacteroides fragilis) e gram-negativos entéricos (ex: Ciprofloxacino + Metronidazol ou Ceftriaxona + Metronidazol). Drenagem percutânea é reservada para abscessos >= 4 cm (Hinchey II). A colonoscopia é estritamente contraindicada na fase aguda por risco de perfuração.',
    'HARD',
    'Colégio Brasileiro de Cirurgiões / WSES',
    TRUE
),
(
    2,
    'Abdome Agudo Obstrutivo - Obstrução Intestinal',
    'FLASHCARD',
    'Qual é a principal etiologia de obstrução mecânica do intestino delgado em pacientes com cirurgias abdominais prévias e qual o achado radiológico característico?',
    'Bridas ou aderências pós-operatórias; o achado clássico no raio-X de abdome em ortostase é a presença de níveis hidroaéreos centrais escalonados/em degrau e empilhamento de moedas (válvulas coniventes).',
    NULL,
    'As aderências peritoneais (bridas) respondem por mais de 60-70% dos casos de obstrução mecânica do intestino delgado no adulto com cirurgia prévia. Na radiografia com o paciente de pé, visualizam-se alças de delgado dilatadas (> 3 cm), distribuídas centralmente com níveis hidroaéreos e ausência de ar no cólon e reto.',
    'MEDIUM',
    'Sabiston Tratado de Cirurgia',
    TRUE
),
(
    2,
    'Abdome Agudo Perfurativo - Úlcera Péptica Perfurada',
    'MULTIPLE_CHOICE',
    'Homem de 40 anos, usuário crônico de anti-inflamatórios não esteroidais (AINEs), apresenta dor abdominal epigástrica súbita, de intensidade máxima desde o início (em facada), que rapidamente se generalizou. Ao exame, abdome tenso, em tábua, doloroso difusamente com descompressão brusca positiva e desaparecimento da macicez hepática à percussão (Sinal de Jobert). Qual o diagnóstico provável e exame confirmatório?',
    'B',
    '["A) Pancreatite aguda grave; confirmação por dosagem sérica de amilase e lipase", "B) Abdome agudo perfurativo por úlcera péptica perfurada; confirmação inicial por Radiografia de Tórax em posição ortostática evidenciando pneumoperitônio sob a cúpula diafragmática", "C) Infarto agudo do miocárdio de parede inferior; confirmação por troponina e cateterismo", "D) Apendicite aguda fase IV; confirmação por ultrassonografia com apêndice dilatado"]'::jsonb,
    'O quadro de dor súbita em facada com abdome em tábua e sinal de Jobert (timpanismo na região hepática por interposição gasosa) é patognomônico de perfuração de víscera oca. O raio-X de tórax em PA com cúpulas diafragmáticas em posição ortostática é o método de triagem mais rápido e eficaz para detecção de pneumoperitônio.',
    'EASY',
    'Sabiston / Residência Médica',
    TRUE
),
(
    2,
    'Hérnias Abdominais - Parede Inguinal',
    'MULTIPLE_CHOICE',
    'Em relação à anatomia da região inguinal e diferenciação das hérnias da virilha, qual marco anatômico separa a hérnia inguinal direta da hérnia inguinal indireta?',
    'A',
    '["A) Vasos epigástricos inferiores: a hérnia indireta localiza-se lateralmente a eles (anel inguinal profundo) e a direta medialmente (trígono de Hesselbach)", "B) Ligamento inguinal: a indireta ocorre abaixo e a direta acima do ligamento", "C) Músculo reto abdominal: a direta invade o anel femoral e a indireta o canal obturatório", "D) Linha alba: a indireta ocorre na linha média e a direta na linha semilunar de Spiegel"]'::jsonb,
    'Os vasos epigástricos inferiores são a referência anatômica primordial: a hérnia inguinal indireta surge lateralmente aos vasos epigástricos inferiores por persistência do conduto peritônio-vaginal pérvio no anel inguinal interno; a hérnia direta surge medialmente a esses vasos, no assoalho do canal inguinal (trígono de Hesselbach), decorrente de fraqueza da fáscia transversalis.',
    'MEDIUM',
    'Anatomia Cirúrgica / ENARE',
    TRUE
),
(
    2,
    'Trauma - Hemotórax Maciço',
    'MULTIPLE_CHOICE',
    'Vítima de trauma torácico fechado por atropelamento é submetida a drenagem tubular fechada de tórax em selo d água à direita por hemotórax. Segundo o ATLS (Advanced Trauma Life Support), qual volume de débito inicial de sangue pelo dreno torácico indica toracotomia cirúrgica de urgência?',
    'C',
    '["A) Débito inicial superior a 300 mL de sangue escuro", "B) Débito de 500 mL associado a fratura de costela isolada", "C) Débito imediato maior ou igual a 1.500 mL de sangue ou sangramento contínuo maior que 200 mL/hora por 2 a 4 horas consecutivas", "D) Qualquer presença de sangue no frasco coletor, independentemente do volume"]'::jsonb,
    'Pelas diretrizes do ATLS 10ª edição, a indicação formal de toracotomia de emergência/urgência no hemotórax decorre de: drenagem imediata de >= 1500 mL de sangue (hemotórax maciço) OU sangramento ativo contínuo persistente superior a 200 mL/hora durante 2 a 4 horas consecutivas associado à instabilidade hemodinâmica.',
    'HARD',
    'ATLS 10ª Edição / American College of Surgeons',
    TRUE
),
(
    2,
    'Trauma - Ultrassonografia FAST',
    'FLASHCARD',
    'Quais são as 4 janelas anatômicas examinadas no protocolo FAST (Focused Assessment with Sonography for Trauma) na sala de emergência?',
    '1. Janela Pericárdica (subxifoide); 2. Janela Hepatorrenal (Espaço de Morison / QSD); 3. Janela Esplenorrenal (QSE); 4. Janela Pélvica / Suprapúbica (fundo de saco de Douglas/retrovesical).',
    NULL,
    'O FAST tem como meta identificar líquido livre (sangue) nos espaços virtuais da cavidade abdominal e pericárdica em pacientes traumatizados. Se for estendido (eFAST), adiciona-se a avaliação dos hemitóraxes anteriores e pleuras para identificação rápida de pneumotórax e hemotórax.',
    'MEDIUM',
    'ATLS 10ª Edição',
    TRUE
),
(
    2,
    'Queimaduras - Ressuscitação Volêmica',
    'MULTIPLE_CHOICE',
    'Paciente masculino de 70 kg é vítima de queimadura térmica de 2º e 3º graus abrangendo tronco anterior (18%) e ambos os membros superiores completos (9% + 9% = 18%), totalizando 36% de Superfície Corporal Queimada (SCQ). Pela fórmula de Parkland modificada recomendada pela American Burn Association (2 mL x kg x %SCQ de Ringer Lactato nas primeiras 24 horas), qual é o volume total a ser infundido e qual a fração que deve correr nas primeiras 8 horas a partir do momento da queimadura?',
    'B',
    '["A) Volume total de 2.500 mL; 100% infundido nas primeiras 8 horas", "B) Volume total de 5.040 mL de Ringer Lactato; metade (2.520 mL) infundida nas primeiras 8 horas e a outra metade nas 16 horas subsequentes", "C) Volume total de 10.000 mL de soro glicosado a 5%; infusão contínua em 24 horas", "D) Volume total de 1.000 mL de coloide com albumina administrado em bólus único"]'::jsonb,
    'Cálculo de Parkland (ABA atualizada): 2 mL x 70 kg x 36% SCQ = 5.040 mL de solução cristaloide isotônica (Ringer Lactato). Conforme a regra do protocolo, 50% desse volume total calculado (2.520 mL) deve ser administrado nas primeiras 8 horas contadas a partir do MOMENTO DO ACIDENTE, e os 50% restantes (2.520 mL) distribuídos ao longo das 16 horas seguintes, ajustando o ritmo para manter débito urinário alvo de 0.5 mL/kg/h.',
    'HARD',
    'American Burn Association / ATLS',
    TRUE
);

-- =============================================================================
-- 3. MEDICINA — 3. PEDIATRIA (subject_id = 3)
-- =============================================================================

INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    3,
    'Puericultura - Marcos do Desenvolvimento',
    'MULTIPLE_CHOICE',
    'Durante a consulta de puericultura de uma criança de 9 meses, o pediatra avalia os marcos do neurodesenvolvimento esperados para a idade. Qual das seguintes habilidades motoras e cognitivas é típica dessa faixa etária?',
    'B',
    '["A) Andar sem apoio e correr com firmeza", "B) Movimento de pinça inferior com o polegar e indicador, sentar sem apoio e emitir polissílabos (balbucio)", "C) Construir torres de 6 cubos e formular frases completas com sujeito e predicado", "D) Controle esfincteriano diurno e noturno estabelecido"]'::jsonb,
    'Aos 9 meses de idade, os marcos clássicos descritos pela SBP incluem: sentar-se sem apoio de forma estável, realização da pinça inferior/rudimentar (polegar e indicador), balbucio polissilábico (dada, mama) e estranhamento de pessoas não familiares. Andar sozinho ocorre por volta dos 12 a 15 meses, e frases completas e controle esfincteriano a partir dos 2 anos.',
    'MEDIUM',
    'Tratado de Pediatria SBP / Nelson',
    TRUE
),
(
    3,
    'Aleitamento Materno - Diretrizes OMS',
    'FLASHCARD',
    'Qual é a recomendação oficial da Organização Mundial da Saúde (OMS) e do Ministério da Saúde do Brasil para o tempo de Aleitamento Materno Exclusivo e Complementado?',
    'Aleitamento materno exclusivo até os 6 meses completos de vida (sem água, chás ou outros alimentos); e aleitamento materno continuado e complementado até os 2 anos de idade ou mais.',
    NULL,
    'O leite materno fornece todos os nutrientes, anticorpos (especialmente IgA secretória) e água necessários para o lactente até os 6 meses, protegendo contra infecções diarreicas e respiratórias agudas. A partir dos 6 meses, inicia-se a introdução de alimentos seguros e adequados mantendo as mamadas.',
    'EASY',
    'Ministério da Saúde / OMS 2024',
    TRUE
),
(
    3,
    'Neonatologia - Reanimação em Sala de Parto',
    'MULTIPLE_CHOICE',
    'Recém-nascido a termo nasce de parto cesariano por descolamento de placenta, hipotônico, sem chorar e em apneia. Após os passos iniciais de reanimação (aquecer, posicionar a cabeça, secar e desprezar campos úmidos) completados em 30 segundos, a criança permanece em apneia e sua frequência cardíaca auscultada é de 76 bpm. Qual é a conduta imediata mandatória?',
    'A',
    '["A) Iniciar Ventilação com Pressão Positiva (VPP) com máscara facial em ar ambiente (O2 a 21%) no primeiro minuto de vida (minuto de ouro)", "B) Iniciar compressões torácicas imediatas na relação 3:1 com oxigênio a 100%", "C) Administrar adrenalina intravenosa pela veia umbilical em bólus rápido", "D) Apenas aspirar vigorosamente vias aéreas profundas e observar por mais 2 minutos"]'::jsonb,
    'Segundo a Diretriz de Reanimação Neonatal da Sociedade Brasileira de Pediatria (SBP), se o RN a termo mantiver apneia, respiração irregular ou FC < 100 bpm após os passos iniciais executados nos primeiros 30 segundos, a conduta imperativa é a Ventilação com Pressão Positiva (VPP). No RN a termo, a VPP é iniciada com ar ambiente (FiO2 21%), garantindo frequência de 40-60 movimentos/minuto.',
    'HARD',
    'SBP Diretrizes de Reanimação Neonatal',
    TRUE
),
(
    3,
    'Infectologia Pediátrica - Doenças Exantemáticas',
    'MULTIPLE_CHOICE',
    'Criança de 3 anos apresenta febre alta há 4 dias associada a tosse produtiva, coriza intensa e conjuntivite com fotofobia. Ao exame da cavidade oral, observam-se pequenos pontos esbranquiçados com halo eritematoso na mucosa jugal, na altura dos molares (manchas de Koplik). No dia seguinte, surge exantema maculopapular avermelhado com início na região retroauricular e linha do couro cabeludo que se estende cranio-caudalmente. Qual é o diagnóstico e agente etiológico?',
    'C',
    '["A) Rubéola - Rubivirus", "B) Eritema Infeccioso - Parvovírus B19", "C) Sarampo - Vírus do Sarampo (família Paramyxoviridae, gênero Morbillivirus)", "D) Escarlatina - Streptococcus pyogenes grupo A"]'::jsonb,
    'As manchas de Koplik são lesões patognomônicas do Sarampo, surgindo cerca de 24 a 48 horas antes do exantema. O quadro catarral intenso (tosse, coriza, conjuntivite) somado à progressão craniocaudal do exantema morbiliforme e descamação furfurácea confirma a infecção pelo Morbillivirus.',
    'MEDIUM',
    'Manual de Vigilância Epidemiológica / MS',
    TRUE
),
(
    3,
    'Pneumologia Pediátrica - Bronquiolite Viral Aguda',
    'MULTIPLE_CHOICE',
    'Lactente de 4 meses é levado à emergência pediátrica com coriza há 2 dias, evoluindo com tosse persistente, taquipneia, tiragem subcostal e sibilos difusos à ausculta pulmonar. Saturação de O2 = 93%. É o primeiro episódio sibilante da criança. Qual o principal agente etiológico e qual a conduta recomendada pelas diretrizes pediátricas?',
    'B',
    '["A) Rinovírus; tratamento obrigatório com broncodilatador inalatório contínuo e corticoide oral", "B) Vírus Sincicial Respiratório (VSR); suporte ventilatório/oxigenoterapia se SpO2 < 90-92%, lavagem nasal com soro fisiológico e hidratação adequada, sem indicação de corticoide ou broncodilatador de rotina", "C) Streptococcus pneumoniae; início imediato de Amoxicilina oral em dose alta", "D) Metapneumovírus; indicação imediata de intubação orotraqueal e sedação profunda"]'::jsonb,
    'A Bronquiolite Viral Aguda (BVA) é a causa mais comum de sibilância e infecção do trato respiratório inferior em lactentes abaixo de 1 ano, sendo o Vírus Sincicial Respiratório (VSR) responsável por até 80% dos casos. Conforme as diretrizes da SBP e da Academia Americana de Pediatria (AAP), o tratamento é essencialmente de suporte (oxigênio se dessaturando, desobstrução de vias aéreas com soro e alimentação fracionada). O uso de broncodilatadores, corticoides sistêmicos e antibióticos NÃO é recomendado de rotina.',
    'MEDIUM',
    'Diretrizes SBP / AAP Bronchiolitis',
    TRUE
),
(
    3,
    'Infectologia Pediátrica - Laringite Aguda / Crupe',
    'FLASHCARD',
    'Qual a tríade clínica clássica da Laringotraqueobronquite Viral Aguda (Crupe) e qual é o tratamento farmacológico padrão nos casos moderados a graves com estridor em repouso?',
    'Tríade: Tosse ladrante/metálica, rouquidão (disfonia) e estridor inspiratório. Tratamento: Nebulização com Adrenalina (epinefrina) 1:1000 + Corticoterapia com Dexametasona oral ou intramuscular.',
    NULL,
    'O Crupe é causado principalmente pelo vírus Parainfluenza humano. Em pacientes com estridor em repouso ou desconforto respiratório, a nebulização com adrenalina causa vasoconstrição local rápida reduzindo o edema subglótico, enquanto a dexametasona garante o efeito anti-inflamatório duradouro prevenindo o efeito rebote.',
    'MEDIUM',
    'SBP / UpToDate Pediátrico',
    TRUE
),
(
    3,
    'Nefrologia Pediátrica - Infecção do Trato Urinário',
    'MULTIPLE_CHOICE',
    'Menina de 8 meses, sem controle esfincteriano, apresenta febre inexplicada (38.8°C) há 3 dias, sem foco infeccioso evidente ao exame físico. Suspeita-se de pielonefrite aguda / ITU febril. Qual método de coleta de urina é o padrão-ouro recomendado pela SBP para confirmar a urocultura nessa paciente?',
    'C',
    '["A) Saco coletor adesivo com troca a cada 30 minutos", "B) Coleta do jato médio após micção espontânea durante o choro", "C) Cateterismo vesical alívio (sondagem vesical) ou punção suprapúbica em condições assépticas", "D) Coleta de urina diretamente da fralda descartável recém-umedecida"]'::jsonb,
    'Em crianças sem controle de esfíncter, o saco coletor tem valor apenas quando a cultura resulta NEGATIVA. Em caso de resultado positivo no saco coletor, a taxa de contaminação fecal/perineal chega a 85%, invalidando o diagnóstico. Portanto, para diagnóstico e confirmação de ITU febril antes de iniciar antibioticoterapia, o cateterismo vesical (sondagem) ou a punção suprapúbica são métodos mandatórios com critérios microbiológicos estritos (>= 50.000 UFC/mL na sondagem).',
    'HARD',
    'Sociedade Brasileira de Pediatria / AAP',
    TRUE
);

-- =============================================================================
-- 4. MEDICINA — 4. GINECOLOGIA E OBSTETRÍCIA (subject_id = 4)
-- =============================================================================

INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    4,
    'Obstetrícia - Assistência Pré-Natal',
    'MULTIPLE_CHOICE',
    'Primigesta de 8 semanas de idade gestacional comparece à primeira consulta de pré-natal de baixo risco na UBS. Além dos exames laboratoriais de rotina do 1º trimestre, quais suplementações vitamínico-minerais são recomendadas formalmente pelo Ministério da Saúde e FEBRASGO para prevenção de defeitos do tubo neural e anemia materna?',
    'A',
    '["A) Ácido Fólico (idealmente iniciado no período pré-concepcional até 12 semanas) e Sulfato Ferroso (a partir da 20ª semana ou confirmação de gestação)", "B) Vitamina A em megadoses diárias e Cálcio isolado em jejum", "C) Zinco, Vitamina E e Vitamina D em altas doses para todas as gestantes", "D) Contraindicar qualquer suplementação durante o primeiro trimestre gestacional"]'::jsonb,
    'O ácido fólico (400 mcg a 5 mg/dia conforme risco) previne defeitos de fechamento do tubo neural (como anencefalia e espinha bífida) quando usado antes da concepção e nas primeiras semanas de embriogênese. A suplementação de ferro elementar (40 mg/dia correspondente a 200 mg de sulfato ferroso) é universalmente recomendada a partir da 20ª semana para suprir a demanda da expansão volêmica e crescimento fetal.',
    'EASY',
    'Ministério da Saúde do Brasil / FEBRASGO',
    TRUE
),
(
    4,
    'Hemorragia da 1ª Metade - Gravidez Ectópica',
    'MULTIPLE_CHOICE',
    'Mulher de 27 anos com atraso menstrual de 7 semanas apresenta dor em cólica na fossa ilíaca direita e discreto sangramento vaginal escuro há 1 dia. Exame físico: hemodinamicamente estável, dor à palpação anexial direita sem sinais de peritonite. Exames: Beta-hCG quantitativo sérico = 3.500 mUI/mL. Ultrassonografia transvaginal revela cavidade uterina vazia e massa anexial íntegra de 2.8 cm em trompa direita, sem batimentos cardíacos embrionários. Qual o diagnóstico e a conduta clínica aceita?',
    'B',
    '["A) Abortamento retido; curetagem uterina imediata", "B) Gravidez ectópica tubária íntegra; elegível para tratamento medicamentoso conservador com Metotrexato intramuscular (dose única)", "C) Doença trofoblástica gestacional; laparotomia exploradora com histerectomia total", "D) Ameaça de aborto; repouso domiciliar e dosagem de progesterona oral"]'::jsonb,
    'Com beta-hCG sérico > 1.500-2.000 mUI/mL (zona discriminatória), a ausência de saco gestacional intrauterino ao USG transvaginal confirma gravidez ectópica. Como a paciente está estável hemodinamicamente, a massa é < 3.5 cm, beta-hCG < 5.000 e não há BCF embrionário, preenchem-se os critérios para tratamento clínico medicamentoso com Metotrexato (50 mg/m² IM), preservando a fertilidade tubária.',
    'HARD',
    'FEBRASGO / ENARE',
    TRUE
),
(
    4,
    'Hemorragia da 2ª Metade - Descolamento Prematuro de Placenta',
    'MULTIPLE_CHOICE',
    'Gestante de 35 semanas, com histórico de pré-eclâmpsia descompensada, dá entrada na maternidade com dor abdominal súbita de forte intensidade, sangramento vaginal escuro em quantidade moderada e parada de movimentos fetais. Ao exame obstétrico: útero hipertônico ("em madeira"), extremamente doloroso, altura uterina maior que a esperada e BCF inaudíveis por doppler. Qual é o diagnóstico e a conduta imediata?',
    'A',
    '["A) Descolamento Prematuro de Placenta (DPP) com óbito fetal; estabilização hemodinâmica da mãe e interrupção rápida da gestação pela via mais rápida (cesariana de urgência ou parto vaginal se iminente)", "B) Placenta prévia centro-total; repouso absoluto em leito e tocólise farmacológica", "C) Rotura de vasa prévia; amniotomia imediata e indução com ocitocina", "D) Trabalho de parto prematuro fisiológico; alta com analgésicos e repouso"]'::jsonb,
    'A tríade de dor abdominal súbita intensa, hipertonia uterina (útero lenhoso) e sangramento vaginal escuro em paciente hipertensa é o quadro patognomônico de Descolamento Prematuro de Placenta (DPP). O hematoma retroplacentário leva a hipóxia fetal aguda e pode causar óbito fetal e coagulação intravascular disseminada (CIVD). A conduta é o esvaziamento uterino imediato visando salvar a vida materna.',
    'HARD',
    'FEBRASGO / Rezende Obstetrícia',
    TRUE
),
(
    4,
    'Hemorragia da 2ª Metade - Placenta Prévia',
    'FLASHCARD',
    'Quais são as características clínicas clássicas do sangramento na Placenta Prévia que a diferenciam do Descolamento Prematuro de Placenta (DPP)?',
    'O sangramento da Placenta Prévia é indolor, de cor vermelho-rutilante (vivo), de início súbito, recidivante, espontâneo, sem hipertonia uterina (tônus uterino normal) e com vitalidade fetal habitualmente preservada.',
    NULL,
    'Na placenta prévia, a placenta implanta-se parcial ou totalmente no segmento inferior do útero cobrindo o orifício interno do colo. O sangramento ocorre pelo descolamento mecânico durante a formação do segmento inferior. O toque vaginal é PROIBIDO pelo risco de hemorragia maciça cataclísmica; o diagnóstico é confirmado por ultrassonografia.',
    'MEDIUM',
    'FEBRASGO Obstetrícia',
    TRUE
),
(
    4,
    'Endocrinologia Ginecológica - Síndrome dos Ovários Policísticos',
    'MULTIPLE_CHOICE',
    'Mulher de 24 anos procura o ginecologista com queixas de irregularidade menstrual (ciclos a cada 45-60 dias), acne facial moderada e aumento de pelos na linha alba e queixo (escore de Ferriman-Gallwey = 10). O ultrassom pélvico demonstra ovários aumentados de volume (> 10 cm³) com múltiplos folículos periféricos medindo entre 2 e 9 mm. Pelos critérios de Rotterdam, para o diagnóstico formal de SOP é necessária a presença de:',
    'C',
    '["A) Presença obrigatória de todos os três critérios de Rotterdam simultaneamente associados a diabetes", "B) Exclusivamente a presença da imagem ultrassonográfica de ovários policísticos", "C) Pelo menos 2 dos 3 critérios: 1) Oligo ou anovulação crônica; 2) Sinais clínicos e/ou laboratoriais de hiperandrogenismo; 3) Ovários policísticos à ultrassonografia, excluídas outras causas (como hiperplasia adrenal congênita e hiperprolactinemia)", "D) Resistência à insulina documentada pelo teste de tolerância oral à glicose"]'::jsonb,
    'Os critérios de Rotterdam (2003/2023) estabelecem que o diagnóstico de Síndrome dos Ovários Policísticos (SOP) requer a presença de pelo menos dois dos três critérios: 1) disfunção ovulatória (oligo/anovulação), 2) hiperandrogenismo (clínico por hirsutismo/acne ou laboratorial por testosterona elevada) e 3) morfologia policística ao USG, sendo indispensável a exclusão prévia de diagnósticos diferenciais.',
    'MEDIUM',
    'Consenso Internacional de SOP / FEBRASGO',
    TRUE
),
(
    4,
    'Ginecologia Oncológica - Rastreamento Câncer de Colo Uterino',
    'FLASHCARD',
    'Qual a recomendação do Ministério da Saúde / INCA para a faixa etária e periodicidade do exame citopatológico do colo uterino (Papanicolau)?',
    'Mulheres (ou pessoas com colo uterino) entre 25 e 64 anos que já iniciaram atividade sexual; o exame deve ser realizado a cada 3 anos, após dois exames anuais consecutivos com resultados normais.',
    NULL,
    'A infecção pelo papilomavírus humano (HPV) oncogênico tem evolução lenta até o carcinoma invasor. Antes dos 25 anos predominam infecções transitórias que regridem espontaneamente. A citologia rastreia lesões precursoras intraepiteliais (NIC II/III) de alto grau para tratamento precoce antes da invasão neoplásica.',
    'EASY',
    'Diretrizes Brasileiras de Rastreamento do Câncer do Colo / INCA',
    TRUE
),
(
    4,
    'Ginecologia Infecciosa - Doença Inflamatória Pélvica',
    'MULTIPLE_CHOICE',
    'Jovem de 21 anos, sexualmente ativa, apresenta dor pélvica de início há 5 dias que piora durante a relação sexual (dispareunia de profundidade) e corrimento vaginal purulento com febre de 38°C. Ao exame ginecológico: colo hiperemiado com saída de secreção purulenta pelo orifício externo e dor lancinante à mobilização do colo uterino e palpação dos anexos. Qual o diagnóstico e o esquema terapêutico empírico ambulatorial preconizado pelo PCDT do Ministério da Saúde?',
    'A',
    '["A) Doença Inflamatória Pélvica (DIP); Ceftriaxona 500 mg IM (dose única) + Doxiciclina 100 mg VO de 12/12h por 14 dias + Metronidazol 500 mg VO de 12/12h por 14 dias", "B) Cistite bacteriana aguda; Fosfomicina 3g dose única", "C) Endometriose profunda; Anticoncepcional combinado contínuo e analgésicos", "D) Vaginose bacteriana simples; Metronidazol gel vaginal por 5 noites"]'::jsonb,
    'A presença de dor à palpação abdominal inferior, dor anexial e dor à mobilização do colo uterino em mulher jovem sexualmente ativa preenche os critérios maiores para Doença Inflamatória Pélvica (DIP). Os principais patógenos são Neisseria gonorrhoeae, Chlamydia trachomatis e anaeróbios vaginais. O PCDT/MS recomenda cobertura tripla ambulatorial: Ceftriaxona (gonococo) + Doxiciclina (clamídia) + Metronidazol (anaeróbios).',
    'HARD',
    'Protocolo Clínico e Diretrizes Terapêuticas (PCDT) IST / MS',
    TRUE
);

-- =============================================================================
-- 5. MEDICINA — 5. MEDICINA DE FAMÍLIA E COMUNIDADE (subject_id = 5)
-- =============================================================================

INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    5,
    'Atenção Primária - Método Clínico Centrado na Pessoa',
    'MULTIPLE_CHOICE',
    'No Método Clínico Centrado na Pessoa (MCCP), desenvolvido por Moira Stewart e amplamente utilizado na Medicina de Família, o médico explora tanto a "doença" (disease) quanto a "experiência de adoecer" (illness). A sigla SIFE (ou FICA) representa quais dimensões da experiência do paciente?',
    'B',
    '["A) Sintomas, Infecções, Farmacologia e Exames complementares", "B) Sentimentos (medos), Ideias (crenças sobre a causa), Funcionalidade (impacto na rotina) e Expectativas (o que espera da consulta)", "C) Situação socioeconômica, Idade, Fatores genéticos e Escolaridade", "D) Suporte familiar, Internações prévias, Cirurgias e Alergias"]'::jsonb,
    'O primeiro componente do MCCP é "Explorando a saúde, a doença e a experiência da doença". Para compreender a experiência subjetiva do paciente com seu adoecimento, o médico explora o acrônimo SIFE: Sentimentos do paciente em relação ao problema, suas Ideias sobre o que está acontecendo, a alteração na sua Funcionalidade/dia a dia e suas Expectativas quanto ao médico e tratamento.',
    'MEDIUM',
    'Medicina Centrada na Pessoa / Stewart / SBMFC',
    TRUE
),
(
    5,
    'Ferramentas de Abordagem Familiar - Genograma e Ecomapa',
    'FLASHCARD',
    'Qual a diferença de finalidade entre o Genograma e o Ecomapa na abordagem familiar da Atenção Primária à Saúde?',
    'O Genograma representa graficamente a estrutura familiar e biológica (laços de parentesco e histórico de saúde) ao longo de pelo menos 3 gerações; o Ecomapa representa as conexões e interações da família com o meio social e comunitário (trabalho, escola, igreja, UBS, amigos).',
    NULL,
    'Ambas são ferramentas fundamentais do médico de família: enquanto o genograma mapeia padrões hereditários, doenças genéticas e relações afetivas internas entre membros da família, o ecomapa visualiza a rede de apoio social e as fontes de estresse ou suporte externas da família.',
    'EASY',
    'Tratado de Medicina de Família e Comunidade / Duncan',
    TRUE
),
(
    5,
    'Bioética e Saúde Pública - Prevenção Quaternária',
    'MULTIPLE_CHOICE',
    'O conceito de Prevenção Quaternária (P4), introduzido pelo médico belga Marc Jamoulle e adotado pela WONCA, é definido como a ação que:',
    'A',
    '["A) Identifica indivíduos em risco de supermedicalização, protegendo-os de intervenções médicas invasivas ou iatrogênicas desnecessárias e sugerindo condutas eticamente aceitáveis", "B) Realiza vacinação e saneamento básico para erradicar doenças transmissíveis na população geral", "C) Detecta precocemente lesões pré-cancerosas por exames de rastreio em indivíduos assintomáticos", "D) Reabilita sequelas pós-AVC em centros de fisioterapia de alta complexidade"]'::jsonb,
    'A Prevenção Quaternária baseia-se no princípio hipocrático primum non nocere (primeiro, não causar dano). Ela busca frear o excesso de diagnóstico (overdiagnosis) e excesso de tratamento (overtreatment), evitando que pacientes recebam exames e procedimentos desnecessários causadores de ansiedade, rotulação e iatrogenia.',
    'MEDIUM',
    'WONCA / Jamoulle / SBMFC',
    TRUE
),
(
    5,
    'Epidemiologia Clínica - Testes Diagnósticos',
    'MULTIPLE_CHOICE',
    'Em um estudo epidemiológico para avaliação de um novo teste rápido de triagem para Dengue em uma Unidade Básica de Saúde, qual propriedade do teste expressa a capacidade de identificar corretamente os verdadeiros doentes entre todos os indivíduos que efetivamente têm a doença?',
    'C',
    '["A) Especificidade", "B) Valor Preditivo Positivo (VPP)", "C) Sensibilidade", "D) Acurácia global"]'::jsonb,
    'Sensibilidade é a proporção de indivíduos com a doença que apresentam resultado positivo no teste [Verdadeiros Positivos / (Verdadeiros Positivos + Falsos Negativos)]. Testes com alta sensibilidade são fundamentais na triagem (screening), pois geram poucos falsos negativos. Especificidade é a capacidade de identificar os verdadeiros saudáveis.',
    'EASY',
    'Epidemiologia Clínica / Fletcher',
    TRUE
),
(
    5,
    'Saúde Mental na APS - Rastreamento de Depressão',
    'FLASHCARD',
    'Qual instrumento padronizado de autoaplicação com 9 itens baseado no DSM-5 é amplamente utilizado na Atenção Primária para triagem, diagnóstico e graduação da gravidade de episódios depressivos?',
    'PHQ-9 (Patient Health Questionnaire-9).',
    NULL,
    'O PHQ-9 avalia os 9 sintomas diagnósticos de depressão maior nas últimas 2 semanas (humor deprimido, anedonia, sono, energia, apetite, culpa, concentração, retardo/agitação psicomotora e ideação suicida), graduando a severidade de leve a grave e auxiliando no monitoramento da resposta terapêutica.',
    'EASY',
    'Ministério da Saúde / Caderno de Atenção Básica nº 34',
    TRUE
);

-- =============================================================================
-- 6. SISTEMAS DE INFORMAÇÃO — 6. ENGENHARIA DE SOFTWARE (subject_id = 6)
-- =============================================================================

INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    6,
    'Princípios SOLID - Open/Closed Principle',
    'MULTIPLE_CHOICE',
    'De acordo com o Open/Closed Principle (OCP) formulado por Bertrand Meyer e consolidado por Robert C. Martin no SOLID, entidades de software (classes, módulos, funções) devem ser:',
    'A',
    '["A) Abertas para extensão, mas fechadas para modificação", "B) Abertas para modificação direta do código-fonte a qualquer momento", "C) Fechadas para criação de subclasses ou interfaces", "D) Dependentes exclusivamente de implementações concretas de baixo nível"]'::jsonb,
    'O OCP preceitua que o comportamento de um módulo deve poder ser estendido sem que haja necessidade de alterar seu código-fonte já testado e consolidado. Isso é viabilizado pelo uso de abstrações, polimorfismo e padrões comportamentais como Strategy, evitando bugs de regressão.',
    'EASY',
    'Clean Architecture / Robert C. Martin',
    TRUE
),
(
    6,
    'Princípios SOLID - Liskov Substitution Principle',
    'MULTIPLE_CHOICE',
    'O Princípio da Substituição de Liskov (LSP) estabelece que se S é um subtipo de T, então os objetos do tipo T podem ser substituídos por objetos do tipo S sem alterar a corretude do programa. Qual das situações abaixo representa uma violação clássica do LSP?',
    'C',
    '["A) Uma classe filha que reutiliza métodos herdados sem alterar seus contratos", "B) Uma interface genérica que define métodos CRUD abstratos", "C) A subclasse Quadrado que herda de Retângulo e lança UnsupportedOperationException ao tentar alterar largura e altura de forma independente", "D) Uma classe abstrata que declara métodos protegidos implementados por subclasses"]'::jsonb,
    'O clássico problema do Quadrado herdando de Retângulo viola o LSP porque em geometria um quadrado altera altura e largura simultaneamente. No código, ao forçar essa regra ou lançar exceções inesperadas para os métodos herdados (setLargura/setAltura), quebra-se a expectativa e os invariantes do cliente que manipula a classe base Retângulo.',
    'MEDIUM',
    'Clean Code / Barbara Liskov',
    TRUE
),
(
    6,
    'Design Patterns GoF - Strategy Pattern',
    'MULTIPLE_CHOICE',
    'Em um sistema de e-commerce, o cálculo de frete varia dependendo da transportadora (Sedex, Jadlog, Loggi, Retirada). Em vez de utilizar uma sequência de blocos if/else ou switch/case dentro de uma única classe, adota-se uma interface CalculadoraFrete implementada por cada transportadora específica. Qual padrão de projeto GoF foi aplicado?',
    'B',
    '["A) Observer", "B) Strategy", "C) Adapter", "D) Decorator"]'::jsonb,
    'O Strategy é um padrão comportamental que define uma família de algoritmos, encapsula cada um deles em uma classe separada e os torna intercambiáveis em tempo de execução, permitindo que o algoritmo varie independentemente dos clientes que o utilizam.',
    'EASY',
    'GoF Design Patterns',
    TRUE
),
(
    6,
    'Arquitetura de Microsserviços - Saga Pattern',
    'FLASHCARD',
    'Qual é o propósito do padrão Saga em arquiteturas de microsserviços distribuídos e quais são as duas abordagens principais para sua coordenação?',
    'Garantir a consistência de dados em transações de negócio distribuídas entre múltiplos microsserviços sem utilizar bloqueios 2PC (Two-Phase Commit), aplicando transações compensatórias em caso de falha. As abordagens são: 1. Orquestração (um orquestrador central coordena os passos) e 2. Coreografia (os serviços se comunicam via eventos de forma descentralizada).',
    NULL,
    'Como cada microsserviço possui seu próprio banco de dados isolado, não é viável usar transações ACID ACID-globais. A Saga quebra a operação em uma sequência de transações locais; se uma falhar, dispara transações compensatórias para reverter as alterações anteriores.',
    'HARD',
    'Microservices Patterns / Chris Richardson',
    TRUE
),
(
    6,
    'Refatoração e Clean Code - Code Smells',
    'MULTIPLE_CHOICE',
    'Em uma revisão de código, identifica-se um método da classe PedidoService que acessa exaustivamente os métodos getters e campos internos da classe Cliente para realizar validações e formatações de dados pessoais. Martin Fowler denomina esse Code Smell de:',
    'D',
    '["A) Shotgun Surgery", "B) Long Parameter List", "C) Data Clump", "D) Feature Envy (Inveja de Recursos)"]'::jsonb,
    'Feature Envy ocorre quando um método em uma classe parece mais interessado nos dados e métodos de outra classe do que na sua própria. A refatoração recomendada por Fowler é Move Method para colocar a lógica dentro da classe dona dos dados (Tell, Don t Ask).',
    'MEDIUM',
    'Refactoring / Martin Fowler',
    TRUE
);

-- =============================================================================
-- 7. SISTEMAS DE INFORMAÇÃO — 7. BANCOS DE DADOS E PERSISTÊNCIA (subject_id = 7)
-- =============================================================================

INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    7,
    'Modelagem Relacional - Terceira Forma Normal (3FN)',
    'MULTIPLE_CHOICE',
    'Uma tabela de pedidos contém os atributos: ID_Pedido (PK), ID_Cliente (FK), Nome_Cliente, Email_Cliente e Data_Pedido. A tabela está na 2FN. Por qual motivo essa tabela NÃO atende aos requisitos da Terceira Forma Normal (3FN)?',
    'A',
    '["A) Porque Nome_Cliente e Email_Cliente dependem transitivamente da chave primária através de ID_Cliente (dependência transitiva de atributos não-chave)", "B) Porque existem atributos multivalorados na mesma coluna", "C) Porque a chave primária é simples e não composta", "D) Porque a tabela não possui índice vetorial cadastrado"]'::jsonb,
    'A 3FN exige que a tabela esteja na 2FN e que nenhum atributo não-chave dependa transitivamente de uma chave candidata. No exemplo, ID_Pedido -> ID_Cliente e ID_Cliente -> Nome_Cliente. Nome_Cliente depende de ID_Cliente (um atributo não-chave), devendo ser separado em uma tabela própria tb_cliente.',
    'MEDIUM',
    'Sistemas de Banco de Dados / Elmasri & Navathe',
    TRUE
),
(
    7,
    'Transações ACID - Níveis de Isolamento ANSI SQL',
    'MULTIPLE_CHOICE',
    'No padrão ANSI/ISO SQL, qual nível de isolamento de transação previne tanto Leitura Suja (Dirty Read) quanto Leitura Não-Repetível (Non-Repeatable Read), mas ainda permite a ocorrência de Leituras Fantasmas (Phantom Reads)?',
    'C',
    '["A) Read Uncommitted", "B) Read Committed", "C) Repeatable Read", "D) Serializable"]'::jsonb,
    'No nível Repeatable Read, leituras repetidas de uma mesma linha retornam sempre os mesmos dados (evitando leitura suja e não repetível), mas inserções de novas linhas feitas por outras transações que se enquadrem no predicado da consulta podem aparecer em nova busca (Leitura Fantasma). Apenas o nível Serializable elimina todas as anomalias.',
    'HARD',
    'Database System Concepts / Silberschatz',
    TRUE
),
(
    7,
    'Otimização de Consultas - Explain Analyze',
    'FLASHCARD',
    'No PostgreSQL, qual é a diferença fundamental entre executar "EXPLAIN <sql>" e "EXPLAIN ANALYZE <sql>"?',
    'EXPLAIN gera apenas o plano de execução estimado pelo otimizador de custos estatísticos sem executar a consulta; EXPLAIN ANALYZE executa a consulta de fato no banco, medindo e retornando o tempo real (actual time) em milissegundos, quantidade de nós percorridos e linhas reais processadas.',
    NULL,
    'O EXPLAIN ANALYZE é essencial para identificar gargalos reais, como Sequential Scans inesperados, disparidades entre linhas estimadas e reais (indicando estatísticas desatualizadas) ou estouro de memória no disco durante ordenações.',
    'MEDIUM',
    'PostgreSQL Official Documentation',
    TRUE
),
(
    7,
    'Sistemas Distribuídos - Teorema CAP',
    'MULTIPLE_CHOICE',
    'O Teorema CAP de Eric Brewer afirma que em um sistema distribuído de armazenamento de dados sujeito a particionamento de rede (Network Partition - P), é matematicamente impossível garantir simultaneamente:',
    'B',
    '["A) Escalabilidade horizontal e Criptografia", "B) Consistência estrita (Consistency) e Disponibilidade (Availability)", "C) Baixa latência e Conectividade Bluetooth", "D) Transações ACID e Modelagem em Grafos"]'::jsonb,
    'Em caso de falha ou particionamento de rede (P), o sistema distribuído é obrigado a escolher entre: 1) Manter a Consistência (CP), bloqueando requisições até restaurar a rede para evitar dados desatualizados (sacrificando disponibilidade); ou 2) Manter a Disponibilidade (AP), respondendo imediatamente mesmo com risco de divergência de dados entre nós.',
    'MEDIUM',
    'Eric Brewer / Distributed Systems',
    TRUE
);

-- =============================================================================
-- 8. SISTEMAS DE INFORMAÇÃO — 8. ESTRUTURAS DE DADOS E ALGORITMOS (subject_id = 8)
-- =============================================================================

INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    8,
    'Algoritmos de Busca - Busca Binária',
    'MULTIPLE_CHOICE',
    'Qual pré-requisito fundamental deve ser atendido por uma coleção de dados para que o algoritmo de Busca Binária possa ser executado com sucesso e qual é sua complexidade assintótica de tempo?',
    'A',
    '["A) A coleção/array deve estar previamente ordenada; a complexidade de tempo é O(log N)", "B) A coleção deve ser uma lista duplamente encadeada não ordenada; complexidade O(N)", "C) O tamanho da coleção deve ser uma potência de 2 estrita; complexidade O(1)", "D) Todos os elementos devem ser números primos distintos; complexidade O(N log N)"]'::jsonb,
    'A Busca Binária divide o espaço de busca pela metade a cada iteração comparando o elemento central com a chave buscada. Para isso, o array deve estar necessariamente ordenado, resultando em complexidade logarítmica O(log N).',
    'EASY',
    'Cormen / Algoritmos: Teoria e Prática',
    TRUE
),
(
    8,
    'Estruturas em Grafos - BFS vs DFS',
    'FLASHCARD',
    'Quais estruturas de dados auxiliares clássicas são empregadas na Busca em Largura (BFS) e na Busca em Profundidade (DFS) em grafos?',
    'A Busca em Largura (BFS) utiliza uma Fila (Queue / FIFO) para explorar vértices por níveis concêntricos; a Busca em Profundidade (DFS) utiliza uma Pilha (Stack / LIFO) ou a própria pilha de chamadas da recursão.',
    NULL,
    'BFS é ideal para encontrar o caminho mais curto em grafos não ponderados (como conexões de redes sociais e roteamento em saltos). DFS é ideal para busca exaustiva de ciclos, ordenação topológica e identificação de componentes conexos.',
    'MEDIUM',
    'Sedgewick / Algorithms 4th Ed',
    TRUE
),
(
    8,
    'Tabelas Hash - Tratamento de Colisões',
    'MULTIPLE_CHOICE',
    'Em tabelas de dispersão (Hash Tables), quando duas chaves distintas produzem o mesmo índice hash (colisão), a técnica de Encadeamento Separado (Separate Chaining) resolve o conflito:',
    'B',
    '["A) Sobrescrevendo o valor anterior e descartando o registro mais antigo", "B) Mantendo uma lista encadeada (ou árvore binária balanceada) em cada posição do array para armazenar múltiplos elementos com o mesmo hash", "C) Percorrendo sequencialmente as posições subsequentes do array até encontrar uma vazia (sondagem linear)", "D) Duplicando imediatamente o tamanho do array e recalculando todas as chaves"]'::jsonb,
    'No Encadeamento Separado (Separate Chaining, padrão usado no Java HashMap), cada balde (bucket) da tabela hash é a cabeça de uma lista encadeada ou árvore rubro-negra (quando o balde atinge >= 8 elementos no Java 8+). A busca no balde compara a chave com equals(). A alternativa C descreve o Endereçamento Aberto com Sondagem Linear.',
    'MEDIUM',
    'Estruturas de Dados e Algoritmos em Java / Lafore',
    TRUE
);

-- =============================================================================
-- 9. SISTEMAS DE INFORMAÇÃO — 9. SEGURANÇA DA INFORMAÇÃO (subject_id = 9)
-- =============================================================================

INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    9,
    'Criptografia - Criptografia Assimétrica',
    'MULTIPLE_CHOICE',
    'Na criptografia de chave pública (assimétrica), quando Alice deseja enviar uma mensagem confidencial para Bob de modo que somente Bob consiga decifrá-la, Alice deve criptografar a mensagem utilizando:',
    'C',
    '["A) A chave privada de Alice", "B) Uma chave secreta compartilhada em canal aberto", "C) A chave pública de Bob", "D) A chave pública de Alice"]'::jsonb,
    'Na criptografia assimétrica com fins de confidencialidade (sigilo), a mensagem é cifrada com a chave pública do destinatário (Bob). Somente a chave privada correspondente de Bob (que ele nunca divulga) tem a capacidade matemática de decifrar o conteúdo. Para assinatura digital e autenticidade, o remetente cifraria com sua própria chave privada.',
    'EASY',
    'Criptografia e Segurança de Redes / William Stallings',
    TRUE
),
(
    9,
    'OWASP Top 10 - Broken Access Control',
    'MULTIPLE_CHOICE',
    'Um usuário autenticado altera o parâmetro da URL de "https://app.com/api/pedidos/100" para "https://app.com/api/pedidos/101" e consegue visualizar os dados confidenciais e detalhes financeiros de outro cliente sem autorização. Qual vulnerabilidade crítica do OWASP Top 10 (posição #1) está caracterizada?',
    'A',
    '["A) Broken Access Control (Controle de Acesso Quebrado / IDOR - Insecure Direct Object References)", "B) Cross-Site Scripting (XSS)", "C) SQL Injection", "D) Server-Side Template Injection (SSTI)"]'::jsonb,
    'Broken Access Control (vulnerabilidade A01:2021 no OWASP Top 10) ocorre quando a aplicação não valida se o usuário autenticado tem permissão explícita para acessar o registro ou entidade identificada no parâmetro ou URL (IDOR). A correção exige validação de autorização baseada no contexto do usuário no backend.',
    'MEDIUM',
    'OWASP Top 10:2021',
    TRUE
),
(
    9,
    'Autenticação e Autorização - OAuth 2.0 com PKCE',
    'FLASHCARD',
    'Por que o fluxo OAuth 2.0 Authorization Code Grant associado a PKCE (Proof Key for Code Exchange) é obrigatório para Single Page Applications (SPAs) e aplicativos móveis nativos?',
    'Porque clientes públicos (SPAs no navegador e apps móveis) não conseguem armazenar com segurança um client_secret confidencial sem expô-lo no código do cliente. O PKCE previne ataques de interceptação do código de autorização através de um par gerado dinamicamente: code_verifier e code_challenge.',
    NULL,
    'No fluxo com PKCE, o cliente cria uma string aleatória secreta (code_verifier) e calcula seu hash SHA-256 (code_challenge). Apenas o cliente que detém o code_verifier original consegue trocar o authorization code pelo access token, neutralizando ataques de interceptação.',
    'HARD',
    'RFC 7636 / OAuth 2.0 Security Best Current Practice',
    TRUE
);

-- =============================================================================
-- 10. SISTEMAS DE INFORMAÇÃO — 10. REDES E SISTEMAS DISTRIBUÍDOS (subject_id = 10)
-- =============================================================================

INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    10,
    'Camada de Transporte - Protocolo TCP vs UDP',
    'MULTIPLE_CHOICE',
    'Em redes de computadores, o protocolo TCP (Transmission Control Protocol) estabelece uma conexão confiável e orientada à conexão através de qual mecanismo de sincronização inicial?',
    'B',
    '["A) Troca de mensagens UDP sem confirmação de recebimento", "B) Three-Way Handshake (Aperto de mão em 3 vias: SYN -> SYN/ACK -> ACK)", "C) Disparo em broadcast para todos os roteadores vizinhos", "D) Negociação de chaves TLS por porta serial física"]'::jsonb,
    'O aperto de mão em três vias (Three-Way Handshake) permite que o cliente e o servidor sincronizem seus números de sequência iniciais e estabeleçam buffers de transmissão antes do envio real de dados úteis: 1) Cliente envia SYN; 2) Servidor responde SYN-ACK; 3) Cliente confirma com ACK.',
    'EASY',
    'Redes de Computadores / Tanenbaum & Wetherall',
    TRUE
),
(
    10,
    'Resiliência em Microsserviços - Circuit Breaker',
    'MULTIPLE_CHOICE',
    'No padrão de resiliência Circuit Breaker (comumente implementado com Resilience4j no ecossistema Spring Boot), quando o disjuntor detecta uma taxa de falhas superior ao limite configurado e transiciona para o estado "OPEN" (Aberto), qual é seu comportamento imediato diante de novas requisições?',
    'A',
    '["A) Rejeita imediatamente as requisições (CallNotPermittedException) sem chamar o serviço de destino instável, ativando o método de fallback instantaneamente", "B) Continua enviando todas as requisições aguardando timeout de 60 segundos", "C) Derruba o servidor da aplicação para forçar reinicialização do pod", "D) Redireciona o tráfego exclusivamente para conexões via satélite"]'::jsonb,
    'O estado ABERTO (OPEN) do Circuit Breaker tem como objetivo prevenir o efeito cascata de exaustão de threads e recursos na aplicação chamadora quando um microsserviço dependente está fora do ar. As chamadas falham instantaneamente sem onerar o socket, acionando o fallback gracioso até que o estado HALF-OPEN teste a recuperação.',
    'MEDIUM',
    'Release It! / Michael Nygard / Resilience4j Docs',
    TRUE
),
(
    10,
    'Mensageria Assíncrona - Idempotência de Consumidores',
    'FLASHCARD',
    'Por que consumidores de mensagens em filas AMQP/RabbitMQ devem ser implementados de forma idempotente em sistemas de produção?',
    'Porque protocolos de mensageria que garantem entrega "at-least-once" (pelo menos uma vez) podem redespachar a mesma mensagem em caso de falhas transitórias de rede, timeouts ou reinicialização de instâncias antes do envio do ACK. A idempotência garante que processar a mesma mensagem múltiplas vezes produza o mesmo resultado sem duplicidades de dados.',
    NULL,
    'Estratégias comuns de idempotência incluem o uso de IDs de mensagem únicos verificados em uma tabela de controle transacional (padrão Idempotent Consumer / Inbox Pattern) ou operações de banco projetadas com UPSERT / ON CONFLICT DO NOTHING.',
    'HARD',
    'Enterprise Integration Patterns / RabbitMQ Best Practices',
    TRUE
);

-- =============================================================================
-- FIM DA MIGRATION V3
-- =============================================================================
