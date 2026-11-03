package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.TagRequest;
import br.com.lrrepresentacoes.connectcrm.dto.TagResponse;
import br.com.lrrepresentacoes.connectcrm.dto.TagUsoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.model.Tag;
import br.com.lrrepresentacoes.connectcrm.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    @Test
    void criar_quandoNomeNaoExiste_salvaNovaTag() {
        when(tagRepository.findByNomeIgnoreCase("Seda")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        TagResponse resposta = tagService.criar(new TagRequest("Seda"));

        assertThat(resposta.nome()).isEqualTo("Seda");
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void criar_quandoNomeJaExisteComOutrasMaiusculas_reaproveitaATagExistente() {
        Tag existente = new Tag("Linho");
        when(tagRepository.findByNomeIgnoreCase("LINHO")).thenReturn(Optional.of(existente));

        TagResponse resposta = tagService.criar(new TagRequest("LINHO"));

        assertThat(resposta.nome()).isEqualTo("Linho");
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void criar_removeEspacosDoInicioEDoFimDoNome() {
        when(tagRepository.findByNomeIgnoreCase("Seda")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        tagService.criar(new TagRequest("  Seda  "));

        ArgumentCaptor<Tag> salva = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(salva.capture());
        assertThat(salva.getValue().getNome()).isEqualTo("Seda");
    }

    @Test
    void listar_devolveAsTagsNaOrdemDoRepository() {
        when(tagRepository.findAllByOrderByNomeAsc())
                .thenReturn(List.of(new Tag("Denim"), new Tag("Linho")));

        List<TagResponse> tags = tagService.listar();

        assertThat(tags).extracting(TagResponse::nome).containsExactly("Denim", "Linho");
    }

    @Test
    void consultarUso_informaEmQuantosClientesATagEsta() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(new Tag("Malha")));
        when(tagRepository.contarClientes(1L)).thenReturn(3L);

        TagUsoResponse uso = tagService.consultarUso(1L);

        assertThat(uso.nome()).isEqualTo("Malha");
        assertThat(uso.quantidadeClientes()).isEqualTo(3L);
    }

    @Test
    void excluir_quandoTagExiste_apagaATag() {
        Tag tag = new Tag("Denim");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));

        tagService.excluir(1L);

        verify(tagRepository).delete(tag);
    }

    @Test
    void excluir_quandoTagNaoExiste_lancaRecursoNaoEncontrado() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.excluir(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Tag não encontrada");
        verify(tagRepository, never()).delete(any(Tag.class));
    }
}
