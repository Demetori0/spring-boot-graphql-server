package com.example.DemoGraphQL.resolver;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import com.example.DemoGraphQL.filter.FilterInput;
import com.example.DemoGraphQL.filter.resolver.RootResolver;
import com.example.DemoGraphQL.filter.resolver.option.OptionsResolver;
import com.example.DemoGraphQL.model.Book;
import com.example.DemoGraphQL.options.Options;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Operator;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.SelectSeekStep1;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;

import static com.example.DemoGraphQL.tables.Author.AUTHOR;
import static com.example.DemoGraphQL.tables.Book.BOOK;


/**
 * To define the operations of the root Query type.
 */
public class Query implements GraphQLQueryResolver {

    @Autowired
    private DSLContext jooq;

    private RootResolver rootResolver;
    private OptionsResolver optionsResolver;

    public Query(RootResolver rootResolver, OptionsResolver optionsResolver) {
        this.rootResolver = rootResolver;
        this.optionsResolver = optionsResolver;
    }

    public Iterable<Book> findAllBooks() {
        return jooq
                .selectFrom(BOOK)
                .orderBy(BOOK.ID.asc())
                .fetch()
                .into(BOOK)
                .into(Book.class);
    }

    public Iterable<Book> findBooks(FilterInput filters) {
        rootResolver.resolve(BOOK, filters);
        Condition condition = rootResolver.getCondition();
        return jooq
                .select(BOOK.fields())
                .from(BOOK)
                .join(AUTHOR).onKey()
                .where(condition)
                .fetch()
                .into(BOOK).into(Book.class);
    }

    public Iterable<Book> findBooks(FilterInput filters, Operator operator, FilterInput author, Options options) {
        rootResolver.resolve(BOOK, filters);
        Condition a = rootResolver.getCondition();

        rootResolver.resolve(AUTHOR, author);
        Condition b = rootResolver.getCondition();

        Condition finalCondition;
        switch (operator) {
            case OR:
                finalCondition = DSL.or(a, b);
                break;
            default:
            case AND:
                finalCondition = DSL.and(a, b);
                break;
        }

        SelectConditionStep<Record> selectWhere = jooq.select(BOOK.fields())
                .from(BOOK)
                .join(AUTHOR).onKey()
                .where(finalCondition);

        if (options == null) {
            return selectWhere
                    .fetch()
                    .into(BOOK).into(Book.class);
        }

        SelectSeekStep1 selectWhereOrderBy = selectWhere.orderBy(optionsResolver.resolveOrderBy(options.getOrderBy()));

        if (options.getLimit() != null) {
            return selectWhereOrderBy
                    .limit(optionsResolver.resolveLimit(options.getLimit()))
                    .fetch()
                    .into(BOOK).into(Book.class);
        }
        return selectWhereOrderBy.fetch().into(BOOK).into(Book.class);
    }

    public long countBooks() {
        return jooq.selectCount().from(BOOK).fetchOne(0, int.class);
    }

    public long countAuthors() {
        return jooq.selectCount().from(AUTHOR).fetchOne(0, int.class);
    }

}
