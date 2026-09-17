package br.edu.unipam.tcc.entity.enums;

public enum ChatIntent {
    /** Quer iniciar uma sessão de revisão de flashcards. */
    START_REVIEW,
    /** Fez uma pergunta de conteúdo que deve ser respondida pelo RAG. */
    ASK_DOUBT,
    /** Quer ver ou alterar suas configurações: apelido, lembrete, curso ou período. */
    OPEN_SETTINGS,
    /** Quer ver o menu de opções. */
    SHOW_MENU,
    /** Quer encerrar a sessão. */
    EXIT
}
