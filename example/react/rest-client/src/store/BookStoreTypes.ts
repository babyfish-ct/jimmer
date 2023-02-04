import { api } from "../common/ApiInstance";
import { ElementOf, ResponseOf } from "../__generated";

export type BookStoreRow = ElementOf<ResponseOf<typeof api.bookStoreService.findStores>>;