package studio.adtech.parquet.msgpack;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

public class CSVAsJSONIterator implements JSONIterator {

    private final CSVHeaderMap headerMap;
    private final CSVParser parser;
    private final Iterator<CSVRecord> inner;
    private final JsonFactory jsonFactory;

    public static CSVAsJSONIterator fromResource(String name, CSVFormat format, CSVHeaderMap headerMap) {
        File file = new File(Thread.currentThread().getContextClassLoader().getResource(name).getPath());
        return new CSVAsJSONIterator(file, format, headerMap);
    }

    public CSVAsJSONIterator(File file, CSVFormat format, CSVHeaderMap headerMap) {
        try {
            this.headerMap = headerMap;
            FileReader reader = new FileReader(file);
            parser = headerMap.injectHeaderFormat(format).parse(reader);
            inner = parser.iterator();
            jsonFactory = new JsonFactory();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        return inner.hasNext();
    }

    @Override
    public String next() {
        CSVRecord record = inner.next();
        StringWriter json = new StringWriter();
        try {
            JsonGenerator gen = jsonFactory.createJsonGenerator(json);
            gen.writeStartObject();
            for (CSVHeaderMap.Entry entry : headerMap.entries()) {
                String name = entry.getName();
                String value = record.get(entry.getIndex());

                gen.writeFieldName(name);
                entry.getWriter().write(gen, value);
            }
            gen.writeEndObject();
            gen.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return json.toString();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        parser.close();
    }
}
