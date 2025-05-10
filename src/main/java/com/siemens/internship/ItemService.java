package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ItemService {
    //added constructor
    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Autowired
    private ItemRepository itemRepository;
    static ExecutorService executor = Executors.newFixedThreadPool(10);
    //changed the list to support async operations
    private List<Item> processedItems = Collections.synchronizedList(new ArrayList<>());
    private AtomicInteger processedCount= new AtomicInteger(0);


    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */

    //Problems with the original implemetation
//    @Async
    //Async not used correctly,the return type must be void or a Future
//    public List<Item> processItemsAsync() {
//
//        List<Long> itemIds = itemRepository.findAllIds();
//
//        for (Long id : itemIds) {
//            CompletableFuture.runAsync(() -> {
//                try {
//                    Thread.sleep(100);
//
//                    Item item = itemRepository.findById(id).orElse(null);
//                    if (item == null) {
//                        return;
//                    }
//                    this is not atomic
//                    processedCount++;
//
//                    item.setStatus("PROCESSED");
//                    itemRepository.save(item);
//                    processedItems.add(item);
//                  two threads could potentially modify the same item if IDs are not unique or stable.
//                } catch (InterruptedException e) {
    //              Inconsistent Error Handling
//                    System.out.println("Error: " + e.getMessage());
//                }
//            }, executor);
//        }
//        the return has no guarantee that all the threads are finished,maybe it will return a incomplete list
//        return processedItems;
//    }
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();
        //every id in the list is mapped to a thread,at the end they are collected
        List<CompletableFuture<Void>> futures = itemIds.stream()
                .map(id -> CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(100);

                        // retrieve the item safely
                        Item item = itemRepository.findById(id).orElseThrow(() ->
                                new IllegalStateException("Item not found for ID: " + id)
                        );
                        item.setStatus("PROCESSED");
                        itemRepository.save(item);
                        //thread-safe increment
                        processedCount.incrementAndGet();
                        // add to the thread-safe list
                        processedItems.add(item);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        // restore the interrupted status
                        throw new RuntimeException("Thread was interrupted", e);
                    } catch (Exception e) {
                        // rethrow to signal error to CompletableFuture
                        throw new CompletionException(e);
                    }
                }, executor))
                .toList();

        // combine all futures into one that completes when all are done
        CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        // when all tasks are complete, return the processed items list
        return allDone.thenApply(v -> processedItems);
    }

}

