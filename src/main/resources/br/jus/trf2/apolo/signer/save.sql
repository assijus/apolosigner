declare 
	-- parametros de entrada
	i_codsecao number(2,0);
	i_coddoc number(10,0);
	i_dthrmov date;
	i_pdfcomprimido blob;
	i_envelopecomprimido blob;
	i_pdfnumpaginas number;
	i_nomeassinante varchar2(100);
  	i_cpf varchar2(14);
  	i_dthrassinatura date;
  	i_dthrultatu date;
  	
  	-- parametros de saida
  	o_status varchar2(32767) := NULL;
  	o_error varchar2(32767) := NULL;
  	
  	-- demais variaveis
  	v_dthrultatu date := NULL;
	v_pdfnumpaginas number := NULL;
  	v_pdfcomprimido blob := null;

  	v_descr varchar2(250);
	v_tipassin number(2,0);
	v_count number;
  	v_seq number(4,0) := NULL;
  	v_seqassin number(4,0) := NULL;
  	v_dthrincl date := NULL;
  	v_login varchar2(200) := NULL;
begin
	i_codsecao := ?;
	i_coddoc := ?;
	i_dthrmov := ?;
	i_pdfcomprimido := ?;
	i_envelopecomprimido := ?;
	i_pdfnumpaginas := ?;
	i_nomeassinante := ?;
	i_cpf := ?;
  	i_dthrassinatura := ?;
  	i_dthrultatu := ?;

  	-- identifica o usuario perante o sistema de auditoria
  	select login into v_login from usuario where numcpf = i_cpf and IndAtivo = 'S';
  	dbms_session_set_context(v_login);
  	
  	-- verifica se o documento ja esta assinado
  	select count(*) into v_count from DocumentoArquivo da where da.CodSecao = i_codsecao and da.CodDoc = i_coddoc and da.DtHrMov = i_dthrmov and da.Numtipmovarq=0;
  	if (v_count > 0) then
		o_status := 'Erro';
  		o_error := 'Documento já estava assinado!';
  	else
  		-- verifica se o pdf e demais dados estao disponiveis na tabela do servico de conversao automatica
  		begin
	  		select DtHrUltAtu, TxtPdfCompr, NumPagesPdf into v_dthrultatu, v_pdfcomprimido, v_pdfnumpaginas from MovimentoTextoPdf mtp where mtp.CodSecao = i_codsecao and mtp.CodDoc = i_coddoc and mtp.DtHrMov = i_dthrmov and mtp.Status='convertidoempdf';
  			--select count(*) from MovimentoTextoPdf mtp where mtp.Status='convertidoempdf';
  			i_pdfcomprimido := v_pdfcomprimido;
  			i_pdfnumpaginas := v_pdfnumpaginas;
  		exception 
  			when NO_DATA_FOUND then
  				-- quando nao estiverem la, nao faz nada
	  			o_status := o_status;
  		end;
  		-- se recebido um parametro com a data da ultima atualizacao, entao vamos apresentar um erro se nao foi possivel recuperar os dados do servico de conversao automatica
		if (i_dthrultatu is not null and i_dthrultatu <> v_dthrultatu) then
			o_status := 'Erro';
  			o_error := 'Documento sofreu alteração!' || i_dthrultatu || ' - ' || v_dthrultatu;
  		else
  			-- obtem a descricao
		  	select 
				(select f.Descr from Fase f where f.CodSecao = m.CodSecao and f.CodFase = m.CodFase) || ' - ' || (select c.Descr from Complemento c where c.CodSecao = m.CodSecao and c.CodCompl = m.CodCompl1)
			into v_descr
			from     
				Movimento m
			where     
				m.CodSecao = i_codsecao
				and m.CodDoc = i_coddoc
				and m.DtHrMov = i_dthrmov;
				
			-- obtem o tipo da assinatura
			v_tipassin := nval_const('$$TipAssin');
			
			-- grava nas tabelas
			documentoarquivo_i(i_codsecao,i_coddoc,v_seq,i_dthrmov,0,i_pdfcomprimido,NULL,NULL,v_descr,i_envelopecomprimido,v_dthrincl,NULL,i_pdfnumpaginas,'N',NULL,NULL,NULL,NULL,NULL,NULL);
			documentoarquivodadosassin_i(v_seqassin,i_codsecao,i_coddoc,v_seq,i_nomeassinante,i_dthrassinatura,v_tipassin);

			-- marca o status da tabela movimentotextopdf com 'assinado'. Precisei arredondar a dthrultatu fazendo um to_char pois estava dando diferente.
			update movimentotextopdf set status='assinado' where codsecao = i_codsecao and coddoc = i_coddoc and dthrmov = i_dthrmov and to_char(dthrultatu, 'dd/mm/yyyy hh24:mi') = to_char(v_dthrultatu, 'dd/mm/yyyy hh24:mi');
			
			o_status := 'OK';
		end if;
  	end if;

	? := o_status;
	? := o_error;
end;