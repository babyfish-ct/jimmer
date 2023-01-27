import { api } from "../common/ApiInstance";
import { MuxRow } from "../common/MuxRow";
import { ElementOf, ResponseOf } from "../__generated";

export type BookStoreRow = MuxRow<ElementOf<ResponseOf<typeof api.bookStoreService.findStores>>>;