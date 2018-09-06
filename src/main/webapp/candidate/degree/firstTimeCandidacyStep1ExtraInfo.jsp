<style>
    input[type="submit"] {
        color: white;
    }
    input[type="submit"]:disabled {
        color: black;
    }
</style>
<script type="text/javascript">
    $(document).ready(function() {
        $('form input[type="submit"]').addClass('btn').addClass('btn-default');
        $('form input[type="submit"]').attr('disabled', 'disabled').addClass('disabled');
        $('input[type="checkbox"]').change(function() {
            if ($('input[type="checkbox"]:checked').size() == 2) {
                $('form input[type="submit"]').removeAttr('disabled').removeClass('disabled');
            } else {
                $('form input[type="submit"]').attr('disabled', 'disabled').addClass('disabled');
            }
        });
    });
</script>
<p style="font-size: 11px; background-color: #faf5de; padding: 10px 20px; border:1px solid #dddddd;">Os dados recolhidos nos seguintes formulários são necessários à sua inscrição e matrícula no Instituto Superior Técnico e serão processados e armazenados informaticamente e em suporte papel. </br> O responsável  pelo  tratamento  dos  seus  dados  pessoais  é  o Instituto Superior Técnico com  o  número  de  pessoa  coletiva 501507930 e sede na Av. Rovisco Pais, 1049-001 Lisboa. </br>Caso necessite de entrar em contacto com o IST poderá fazê-lo através dos seguintes meios: Tel: +351 218 417 000 mail@tecnico.ulisboa.pt. </br>A informação  fornecida  será  tratada  de  forma  confidencial  e  utilizada  exclusivamente  para os seguintes fins: [1] Fins  de  gestão  escolar (Necessário ao exercício de funções de interesse público) [2] Candidatura/Matrícula/Inscrição (Necessário para a execução de um contrato com o titular dos dados) [3] Emissão de Certificados, Certidões e Diplomas (Necessário para a execução de um contrato com o titular dos dados) [4] Fornecimento de dados à Direção-Geral de Estatísticas da Educação e Ciência (Necessário para o cumprimento de obrigações jurídicas) [5] Fornecimento de dados à Universidade de Lisboa (Necessário ao exercício de funções de interesse público) [6] Produção de estatísticas (Necessário ao exercício de funções de interesse público)</p>

<div style="margin-bottom: 1rem;">
    <div style="display: flex; justify-content: center; align-items: center; font-size: 10px; background-color: #edffd0; padding: 10px 20px; border:1px solid #dddddd;">
        <div style="flex: 0 0 30px;">
            <input class="agreement" id="agreement1" type="checkbox"/>
        </div>
        <label for="agreement1" style="font-size: 11px;">
            No âmbito do Protocolo existente entre o Instituto Superior Técnico e o Santander Totta, a emissão do cartão de identificação é feita com a cooperação da entidade bancária, com benefícios para os alunos e restantes utentes da Escola. O cartão de identificação é obrigatório e totalmente gratuito, sendo a sua utilização essencial para o bom funcionamento dos serviços da Escola. </br>Assim, declaro que tomei conhecimento que o Instituto Superior Técnico enviará ao Santander Totta os seguintes dados, para efeitos de emissão e utilização do cartão de identificação do Instituto Superior Técnico:Técnico ID, Nome completo do aluno, Tipo, número e emissão/validade do documento de identificação, Nacionalidade, Morada, País de residência, Data de nascimento, Naturalidade: Distrito,Concelho e Freguesia, Email, Gênero, Estado Civil, Nome do Pai e da Mãe, Telefone/Telemóvel, Tipo e Nome do Curso, Ano Curricular
        </label>
    </div>
</div>

<div style="margin-bottom: 1rem;">
    <div style="display: flex; justify-content: center; align-items: center; font-size: 10px; background-color: #edffd0; padding: 10px 20px; border:1px solid #dddddd;">
        <div style="flex: 0 0 30px;">
            <input class="agreement" id="agreement2" type="checkbox"/>
        </div>
        <label for="agreement2" style="font-size: 11px;">
            No âmbito do Protocolo existente entre a Universidade de Lisboa e a Caixa Geral de Depósitos , a emissão do cartão de identificação é feita com a cooperação da entidade bancária, com benefícios para os alunos e restantes utentes da Escola. O cartão de identificação é obrigatório e totalmente gratuito, sendo a sua utilização essencial para a identificação do aluno noutras escolas da Universidade e acesso às unidades alimentares (cantinas) dos Serviços de Acção Social da Universidade de Lisboa. </br> Assim, declaro que tomei conhecimento que o Instituto Superior Técnico enviará à Caixa Geral de Depósitos os seguintes dados, para efeitos de emissão e utilização do cartão de identificação do Instituto Superior Técnico: Técnico ID , Nome do aluno , Tipo, número e emissão/validade do documento de identificação , Nacionalidade , Data de nascimento , Identificação fiscal , Nome e Tipo do curso , Ano curricular
        </label>
    </div>
</div>