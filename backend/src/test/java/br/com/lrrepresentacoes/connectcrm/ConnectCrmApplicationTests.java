package br.com.lrrepresentacoes.connectcrm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ConnectCrmApplicationTests {

	@Test
	void contextLoads() {
	}

	// TEMPORARIO: teste quebrado de proposito para validar que o CI fica vermelho (issue #12).
	@Test
	void testeQuebradoDeProposito() {
		assertEquals(1, 2, "Teste quebrado de proposito para validar o CI");
	}

}
