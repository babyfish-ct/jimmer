import { api } from "../common/ApiInstance";
import { MuxRow } from "../common/MuxRow";
import { ElementOf, RequestOf, ResponseOf } from "../__generated";

export type SimpleBook = MuxRow<ElementOf<ResponseOf<typeof api.bookService.findSimpleBooks>["content"]>>;

export type ComplexBook = MuxRow<ElementOf<ResponseOf<typeof api.bookService.findComplexBooks>["content"]>>;

export type BookInput = RequestOf<typeof api.bookService.saveBook>["body"];

export function toBookInput(book: ComplexBook): BookInput {
    return {
        id: book.id,
        name: book.name,
        edition: book.edition,
        price: book.price,
        storeId: book.store?.id,
        authorIds: book.authors.map(it => it.id)
    }
}