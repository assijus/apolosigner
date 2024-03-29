package br.jus.trf2.apolo.signer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.pdfbox.pdmodel.PDDocument;

import com.crivano.swaggerservlet.SwaggerServlet;
import com.crivano.swaggerservlet.SwaggerUtils;
import com.crivano.swaggerservlet.dependency.TestableDependency;

import br.jus.trf2.assijus.system.api.IAssijusSystem;

public class ApoloSignerServlet extends SwaggerServlet {
	private static final long serialVersionUID = -1611417120964698257L;
	public static String servletContext = null;

	public static ApoloSignerServlet INSTANCE = null;

	public static String getProp(String name) {
		return INSTANCE.getProperty(name);
	}

	@Override
	public void initialize(ServletConfig config) throws ServletException {
		this.INSTANCE = this;
		setAPI(IAssijusSystem.class);
		setActionPackage("br.jus.trf2.apolo.signer");
		setApiContextClass(AssijusSystemContext.class);

		// Redis
		//
		addRestrictedProperty("redis.database", "10");
		addPrivateProperty("redis.password", null);
		addRestrictedProperty("redis.slave.port", "0");
		addRestrictedProperty("redis.slave.host", null);
		addRestrictedProperty("redis.master.host", "localhost");
		addRestrictedProperty("redis.master.port", "6379");
		SwaggerUtils.setCache(new MemCacheRedis());

		addRestrictedProperty("pdfservice.url", null);

		addRestrictedProperty("datasource.name", "java:/jboss/datasources/ApoloDS");
		addRestrictedProperty("datasource.url", null);
		addRestrictedProperty("datasource.username", null);
		addPrivateProperty("datasource.password", null);
		addRestrictedProperty("datasource.schema", "testeapolotrf");

		addPrivateProperty("password", null);
		super.setAuthorization(getProperty("password"));

		addDependency(new TestableDependency("database", "apolods", false, 0, 10000) {
			@Override
			public String getUrl() {
				return getProperty("datasource.name");
			}

			@Override
			public boolean test() throws Exception {
				Utils.getConnection().close();
				return true;
			}
		});

		addDependency(new TestableDependency("process", "conversor-batch", true, 0, 10000) {
			@Override
			public String getUrl() {
				return getProperty("datasource.name") + "/batch-conv";
			}

			@Override
			public boolean test() throws Exception {
				Connection conn = null;
				PreparedStatement pstmt = null;
				ResultSet rset = null;
				try {
					conn = Utils.getConnection();
					pstmt = conn.prepareStatement(Utils.getSQL("test-batch-conv"));
					rset = pstmt.executeQuery();
					rset.next();
					int count = rset.getInt(1);
					if (count > 0)
						throw new Exception("Existem " + count + " arquivos pendentes de conversão há mais de 1 hora");
				} finally {
					if (rset != null)
						rset.close();
					if (pstmt != null)
						pstmt.close();
					if (conn != null)
						conn.close();
				}
				return true;
			}
		});

		addDependency(new TestableDependency("webservice", "conversor", false, 0, 10000) {

			@Override
			public String getUrl() {
				return getProperty("pdfservice.url");
			}

			@Override
			public boolean test() throws Exception {
				String docx = "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";
				byte pdf[] = Utils.convertDocToPdf(SwaggerUtils.base64Decode(docx));
				int pagecount = PDDocument.load(pdf).getNumberOfPages();
				if (pagecount < 1)
					throw new Exception(
							"Não foi possível contar o número de páginas do PDF, provavelmente o documento está corrompido.");
				return pdf != null;
			}

		});

		addDependency(new TestableDependency("cache", "redis", false, 0, 10000) {

			@Override
			public String getUrl() {
				return "redis://" + MemCacheRedis.getMasterHost() + ":" + MemCacheRedis.getMasterPort() + "/"
						+ MemCacheRedis.getDatabase() + " (" + "redis://" + MemCacheRedis.getSlaveHost() + ":"
						+ MemCacheRedis.getSlavePort() + "/" + MemCacheRedis.getDatabase() + ")";
			}

			@Override
			public boolean test() throws Exception {
				String uuid = UUID.randomUUID().toString();
				MemCacheRedis mc = new MemCacheRedis();
				mc.store("test", uuid.getBytes());
				String uuid2 = new String(mc.retrieve("test"));
				return uuid.equals(uuid2);
			}
		});

	}
}
