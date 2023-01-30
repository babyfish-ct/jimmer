import { api } from "../common/ApiInstance";
import { ElementOf, RequestOf, ResponseOf } from "../__generated";

export type SimpleBook = ElementOf<ResponseOf<typeof api.bookService.findSimpleBooks>>;

export type BookRow = ElementOf<ResponseOf<typeof api.bookService.findBooks>["content"]>;

export type BookInput = RequestOf<typeof api.bookService.saveBook>["body"];

export function toBookInput(book: BookRow): BookInput {
    return {
        id: book.id,
        name: book.name,
        edition: book.edition,
        price: book.price,
        storeId: book.store?.id,
        authorIds: book.authors.map(it => it.id)
    }
}